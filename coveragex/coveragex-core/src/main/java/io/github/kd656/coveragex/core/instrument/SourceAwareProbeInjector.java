package io.github.kd656.coveragex.core.instrument;

import io.github.kd656.coveragex.core.collect.CommonCoverageDataCollector;
import io.github.kd656.coveragex.api.data.ProbeMetadata;
import io.github.kd656.coveragex.core.analysis.source.model.ClassModel;
import io.github.kd656.coveragex.core.analysis.source.model.MethodModel;
import io.github.kd656.coveragex.core.analysis.source.model.MethodReference;
import io.github.kd656.coveragex.core.analysis.source.model.OperandModel;
import io.github.kd656.coveragex.core.probe.SourceAwareBranchResolver;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Source-aware probe injector that populates {@code BranchProbe.conditionText} with
 * verbatim source text from the coverage map instead of generic opcode placeholders.
 *
 * <p>This injector is used when {@link ClassModel} for the class being instrumented
 * is available from the pre-computed coverage map ({@code coveragex.map.json}). It
 * takes a {@link SourceAwareInput} that bundles the raw class bytes with the parsed
 * class model.</p>
 *
 * <h2>Matching algorithm</h2>
 * <p>Method matching is exact: ASM provides {@code (name, descriptor)} at
 * {@code visitMethod} time, which maps directly to the {@link MethodReference} key
 * stored in {@link ClassModel}.</p>
 *
 * <p>Branch matching uses line number + per-line jump index:
 * <ol>
 *   <li>Track the count of conditional jumps seen on each source line
 *       ({@code jumpCountPerLine}).</li>
 *   <li>On each {@code visitJumpInsn}, look up the {@link DecisionModel} whose
 *       {@code conditionRange.beginLine()} matches {@code currentLine}.</li>
 *   <li>Use the accumulated jump count (0-based) as the operand index into
 *       {@link DecisionModel#operands()}.</li>
 *   <li>Read {@link OperandModel#conditionText()} from the matched operand.</li>
 *   <li>Fall back to {@link ProbeInjectionSupport#opcodeToConditionText(int)} at
 *       any step that fails (method not found, decision not found, operand index
 *       out of bounds, conditionText null).</li>
 * </ol>
 * </p>
 *
 * <p>This algorithm relies on {@code javac}'s left-to-right short-circuit evaluation
 * order (JLS §15.23–15.24), which guarantees that conditional jumps are emitted in
 * textual source order. For non-javac compilers (Kotlin, Groovy) the ordering may
 * differ and the fallback will engage.</p>
 */
public class SourceAwareProbeInjector implements ProbeInjector<SourceAwareInput> {

    private static final Logger LOG = LoggerFactory.getLogger(SourceAwareProbeInjector.class);

    private final CommonCoverageDataCollector collector;

    /**
     * Constructs a {@code SourceAwareProbeInjector} backed by the given collector.
     *
     * @param collector the collector that receives class registrations
     */
    public SourceAwareProbeInjector(CommonCoverageDataCollector collector) {
        this.collector = collector;
    }

    /**
     * Injects coverage probes into the given class bytecode using source map data for
     * accurate condition text in branch probes.
     *
     * @param className the internal class name (e.g. {@code org/example/Foo})
     * @param input     the class bytes paired with the pre-loaded {@link ClassModel}
     * @return the instrumented class bytes
     */
    @Override
    public byte[] injectProbes(String className, SourceAwareInput input) {
        LOG.trace("Injecting source-aware probes into: {}", className);

        var reader = new ClassReader(input.classBytes());
        var writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES);

        AtomicInteger probeCounter = new AtomicInteger(0);
        List<ProbeMetadata> metadataAccumulator = new ArrayList<>();

        var visitor = new SourceAwareClassVisitor(
                Opcodes.ASM9, writer, className, input.classModel(),
                probeCounter, metadataAccumulator);

        reader.accept(visitor, ClassReader.EXPAND_FRAMES);

        int totalProbes = probeCounter.get();
        if (totalProbes > 0) {
            collector.registerClass(className, totalProbes, metadataAccumulator);
            LOG.debug("Source-aware instrumented {} with {} probes", className, totalProbes);
        }

        return writer.toByteArray();
    }

    // =========================================================================
    // ClassVisitor
    // =========================================================================

    private static class SourceAwareClassVisitor extends ClassVisitor {

        private final String className;
        private final ClassModel classModel;
        private final AtomicInteger probeCounter;
        private final List<ProbeMetadata> metadataAccumulator;

        SourceAwareClassVisitor(int api, ClassVisitor cv, String className,
                                ClassModel classModel,
                                AtomicInteger probeCounter,
                                List<ProbeMetadata> metadataAccumulator) {
            super(api, cv);
            this.className = className;
            this.classModel = classModel;
            this.probeCounter = probeCounter;
            this.metadataAccumulator = metadataAccumulator;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                        String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

            // Skip abstract methods (no code to instrument)
            if ((access & Opcodes.ACC_ABSTRACT) != 0 || mv == null) {
                return mv;
            }

            // Look up the MethodModel by exact (name, descriptor) key.
            // Returns null for synthetic bridge methods or any method not found in the map,
            // in which case the visitor gracefully falls back to opcode placeholders.
            MethodReference ref = new MethodReference(name, descriptor);
            MethodModel methodModel = classModel.getMethods().get(ref);

            if (methodModel == null) {
                LOG.trace("Method not in source map, using opcode fallback: {}.{}{}", className, name, descriptor);
            }

            return new SourceAwareMethodVisitor(
                    api, mv, className, name, descriptor, access,
                    methodModel, probeCounter, metadataAccumulator);
        }
    }

    // =========================================================================
    // MethodVisitor
    // =========================================================================

    /**
     * MethodVisitor that overrides {@link #resolveConditionText(int, int)} to look up
     * the source-derived condition text from the decision model before falling back to
     * the opcode placeholder.
     *
     * <p>Also overrides {@link #isJumpTakenWhenTrue(int)} to use the per-operand
     * {@link OperandModel#jumpMeansTrue()} flag from the source model. This flag encodes
     * whether the bytecode jump fires when the source-level condition is TRUE (operands in
     * an {@code ||} chain) or FALSE (operands in an {@code &&} chain). Using this flag
     * allows the source-aware path to assign TRUE/FALSE probe labels correctly for both
     * {@code x == null ||} and {@code x != null &&} patterns, which the opcode alone
     * cannot distinguish.</p>
     *
     * <p>{@link SourceAwareBranchResolver} tracks how many conditional jumps have been
     * encountered on each source line within this method. Combined with the source line
     * from {@link #currentLine}, this identifies the corresponding {@link OperandModel}
     * in the source map.</p>
     */
    private static class SourceAwareMethodVisitor extends ProbeInjectionSupport {

        /**
         * Source map model for this method, or {@code null} when the method was not
         * found in the coverage map (synthetic methods, bridge methods, etc.).
         */
        private final MethodModel methodModel;

        private final SourceAwareBranchResolver branchResolver;

        /**
         * Polarity resolved by {@link #resolveConditionText} for the operand currently
         * being processed. Set to the operand's {@link OperandModel#jumpMeansTrue()} when
         * a source-map match is found; {@code null} when the fallback path is taken.
         * Consumed (and cleared) by the immediately following {@link #isJumpTakenWhenTrue}
         * call within the same {@code visitJumpInsn} invocation.
         */
        private Boolean pendingJumpMeansTrue = null;

        SourceAwareMethodVisitor(int api, MethodVisitor mv, String className,
                                 String methodName, String descriptor, int access,
                                 MethodModel methodModel,
                                 AtomicInteger probeCounter,
                                 List<ProbeMetadata> metadataAccumulator) {
            super(api, mv, className, methodName, descriptor, access,
                    probeCounter, metadataAccumulator);
            this.methodModel = methodModel;
            this.branchResolver = new SourceAwareBranchResolver(methodModel);
        }

        /**
         * Resolves the human-readable condition text for a branch at {@code branchLine}
         * using the source map, with graceful fallback to the opcode placeholder.
         *
         * <p>Algorithm (see design doc §3.2):</p>
         * <ol>
         *   <li>If no {@link MethodModel} is available, return opcode placeholder.</li>
         *   <li>Compute the 0-based jump index for this source line and increment the counter.</li>
         *   <li>Find the {@link DecisionModel} whose {@code conditionRange.beginLine()} matches
         *       {@code branchLine}.</li>
         *   <li>Index into {@link DecisionModel#operands()} using the jump index.</li>
         *   <li>Return {@link OperandModel#conditionText()} if non-null; otherwise fall back.</li>
         * </ol>
         *
         * @param opcode     the ASM branch opcode
         * @param branchLine the source line of the jump instruction
         * @return non-null condition text string
         */
        @Override
        protected String resolveConditionText(int opcode, int branchLine) {
            SourceAwareBranchResolver.ResolvedBranch branch = branchResolver.resolve(opcode, branchLine);
            // Resolve condition text and polarity together so both runtime and static planning agree.
            pendingJumpMeansTrue = branch.jumpMeansTrue();
            return branch.conditionText();
        }

        /**
         * Substitutes the AST-derived declaration line from the source map for the
         * bytecode-derived {@link #methodStartLine} before delegating to the base
         * implementation.
         *
         * <p>{@link ProbeInjectionSupport#methodStartLine} is set from the first
         * {@code LineNumberTable} entry inside the method body, which {@code javac}
         * places on the first <em>executable statement</em> — not the method
         * declaration line. {@link MethodModel#getStartLine()} is derived from the
         * AST position of the declaration itself and is therefore correct.</p>
         */
        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            if (methodModel != null) {
                methodStartLine = methodModel.getStartLine();
            }
            super.visitMaxs(maxStack, maxLocals);
        }

        /**
         * Returns the jump polarity from the source model when available, falling back
         * to the opcode-level table in {@link ProbeInjectionSupport}.
         *
         * <p>{@link #resolveConditionText} sets {@link #pendingJumpMeansTrue} immediately
         * before this method is called (both are invoked from the same
         * {@code visitJumpInsn} call). The value is consumed here and cleared so it
         * cannot bleed into a subsequent instruction.</p>
         */
        @Override
        protected boolean isJumpTakenWhenTrue(int opcode) {
            if (pendingJumpMeansTrue != null) {
                boolean result = pendingJumpMeansTrue;
                pendingJumpMeansTrue = null;
                return result;
            }
            return super.isJumpTakenWhenTrue(opcode);
        }
    }
}

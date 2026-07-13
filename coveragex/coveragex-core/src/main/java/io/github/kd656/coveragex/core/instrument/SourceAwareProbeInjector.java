package io.github.kd656.coveragex.core.instrument;

import io.github.kd656.coveragex.api.data.OperandKind;
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
import org.objectweb.asm.RecordComponentVisitor;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        private boolean recordClass;
        private final Map<String, String> recordComponents = new HashMap<>();

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
        public void visit(int version, int access, String name, String signature,
                          String superName, String[] interfaces) {
            recordClass = RecordMethods.isRecord(access);
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public RecordComponentVisitor visitRecordComponent(String name, String descriptor, String signature) {
            if (recordClass) recordComponents.put(name, descriptor);
            return super.visitRecordComponent(name, descriptor, signature);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                        String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

            // Skip abstract methods (no code to instrument)
            if ((access & Opcodes.ACC_ABSTRACT) != 0 || mv == null
                    || (recordClass && RecordMethods.isGeneratedObjectMethod(name, descriptor))) {
                return mv;
            }

            // Look up the MethodModel by exact (name, descriptor) key.
            // Returns null for synthetic bridge methods or any method not found in the map,
            // in which case the visitor gracefully falls back to opcode placeholders.
            MethodReference ref = new MethodReference(name, descriptor);
            MethodModel methodModel = classModel.getMethods().get(ref);

            // Skip compiler-generated record component accessors: they match the shape
            // AND have no source-map entry. A user-written override matches the shape
            // but WILL be in the source map, so it stays instrumented.
            if (recordClass && methodModel == null
                    && RecordMethods.isComponentAccessorShape(name, descriptor, recordComponents)) {
                return mv;
            }

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
            // Populate parameter names from the source map so visitMaxs can include
            // them in the MethodProbe. Falls back to empty list when no model is available.
            if (methodModel != null) {
                pendingParameterNames = methodModel.getParameterNames();
            }
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
            // Populate the pending fields so ProbeMetadataVisitor writes them into BranchProbe.
            pendingConditionId = branch.conditionId();
            pendingKind = branch.kind();
            pendingArgLabels = branch.argLabels();
            return branch.conditionText();
        }

        /**
         * Intercepts method-invocation instructions to capture receiver and argument
         * values for {@link OperandKind#METHOD_CALL} operands before the call site
         * consumes them.
         *
         * <p>The intercept fires when all four conditions hold:</p>
         * <ol>
         *   <li>{@link #pendingOperandLocals} is {@code null} (no prior capture
         *       this operand — the guard prevents a nested call from re-firing after
         *       the outer call has already been captured).</li>
         *   <li>The next pending operand on {@link #currentLine} has
         *       {@code kind == METHOD_CALL}.</li>
         *   <li>The visited call's simple name matches
         *       {@link OperandModel#methodCallName()} — rules out unrelated nested
         *       calls such as {@code resolveUser()} inside
         *       {@code service.canAccess(resolveUser(), role)}.</li>
         *   <li>The visited call's descriptor arity matches
         *       {@link OperandModel#methodCallArgCount()} — guards against overload
         *       collisions when the inner call happens to share a name.</li>
         * </ol>
         *
         * <p>When the capture mask derived from the analyser is zero (e.g. a
         * literal-only call like {@code Rules.enabled("A")}),
         * {@link #captureMethodCallOperand} returns an empty array, and
         * {@code pendingOperandLocals} remains {@code null}; the branch probe is
         * then emitted with an empty {@code Object[]} via
         * {@link #injectBranchProbeCall}. No special-case code is needed for this
         * path.</p>
         *
         * <p>For all other method calls the method delegates to the super chain
         * without any side effect.</p>
         *
         * @param opcode      the invocation opcode (INVOKEVIRTUAL, INVOKEINTERFACE,
         *                    INVOKESTATIC, INVOKESPECIAL)
         * @param owner       the internal name of the class that owns the method
         * @param name        the simple method name
         * @param descriptor  the JVM method descriptor
         * @param isInterface {@code true} when the invocation uses INVOKEINTERFACE
         */
        @Override
        public void visitMethodInsn(int opcode, String owner, String name,
                                    String descriptor, boolean isInterface) {
            if (pendingOperandLocals == null) {
                OperandModel pending = branchResolver.peekNextOperand(currentLine);
                if (pending != null
                        && pending.kind() == OperandKind.METHOD_CALL
                        && name.equals(pending.methodCallName())
                        && Type.getArgumentTypes(descriptor).length == pending.methodCallArgCount()) {
                    int[] captured = captureMethodCallOperand(
                            opcode, descriptor, pending.methodCallCaptureMask());
                    if (captured.length > 0) {
                        pendingOperandLocals = captured;
                    }
                }
            }
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }

        /**
         * Intercepts the category-2 comparison instructions ({@code LCMP},
         * {@code DCMPL} / {@code DCMPG}, {@code FCMPL} / {@code FCMPG}) so that
         * both operands can be boxed before the CMP collapses them into a
         * single {@code -1/0/+1} int.
         *
         * <p>For {@code long}, {@code double}, and {@code float} comparisons,
         * javac emits {@code CMP} followed by a single-operand {@code IF*}. By
         * the time {@link #visitJumpInsn} runs, only the CMP result is on the
         * stack — the original operands are gone. This override captures at the
         * CMP site (both operands still present), stashes them in local slots,
         * and populates {@link #pendingOperandLocals} so the downstream jump
         * handler emits the branch probe with the real values instead of a
         * mislabelled CMP result. If the source model is absent or the pending
         * operand is not a var-vs-var {@code BINARY_COMPARE}, the visit passes
         * through unchanged.</p>
         *
         * @param opcode the instruction opcode (zero-operand insn)
         */
        @Override
        public void visitInsn(int opcode) {
            if (pendingOperandLocals == null
                    && isCategory2CompareOpcode(opcode)) {
                OperandModel pending = branchResolver.peekNextOperand(currentLine);
                if (pending != null
                        && pending.kind() == OperandKind.BINARY_COMPARE
                        && pending.binaryCaptureMask() == 0b11) {
                    int[] captured = captureCategory2ComparisonOperands(
                            opcode, pending.binaryCaptureMask());
                    if (captured.length > 0) {
                        pendingOperandLocals = captured;
                    }
                }
            }
            super.visitInsn(opcode);
        }

        private static boolean isCategory2CompareOpcode(int opcode) {
            return opcode == Opcodes.LCMP
                    || opcode == Opcodes.DCMPL || opcode == Opcodes.DCMPG
                    || opcode == Opcodes.FCMPL || opcode == Opcodes.FCMPG;
        }

        /**
         * Intercepts conditional-jump instructions to capture stack operands for
         * {@link OperandKind#BINARY_COMPARE} operands before the original jump
         * instruction has a chance to consume them.
         *
         * <p>The intercept fires when the next pending branch on {@link #currentLine}
         * has {@code kind == BINARY_COMPARE} and the opcode is a conditional jump.
         * Stack values are stashed into fresh local slots; the slot indices are stored
         * in {@link #pendingOperandLocals} so that the resulting
         * {@link #onBranchProbe} call can include them in the {@code recordBranchHit}
         * invocation.</p>
         *
         * <p>The override also handles the {@link OperandKind#METHOD_CALL} case for
         * {@code equals}-style comparisons: if {@link #pendingOperandLocals} is
         * already set (from an earlier {@link #visitMethodInsn} intercept), this
         * method leaves it untouched, since the capture already happened.</p>
         *
         * @param opcode the conditional-jump opcode
         * @param label  the original jump target
         */
        @Override
        public void visitJumpInsn(int opcode, org.objectweb.asm.Label label) {
            // Only intercept conditional jumps; GOTO and JSR pass through directly.
            if (io.github.kd656.coveragex.core.probe.ProbeOpcodeSupport.isBranchInstruction(opcode)
                    && pendingOperandLocals == null) {
                OperandModel pending = branchResolver.peekNextOperand(currentLine);
                if (pending != null) {
                    pendingOperandLocals = captureForOperand(pending, opcode);
                }
            }
            super.visitJumpInsn(opcode, label);
        }

        /**
         * Selects the capture strategy that matches the operand kind currently
         * being processed.
         *
         * @param pending the next operand on {@link #currentLine} as resolved
         *                from the source map
         * @param opcode  the conditional-jump opcode about to execute
         * @return the captured local slots, or {@code null} when no capture
         *         is performed
         */
        private int[] captureForOperand(OperandModel pending, int opcode) {
            return switch (pending.kind()) {
                case BINARY_COMPARE -> pending.binaryCaptureMask() != 0
                        ? captureBinaryComparisonOperand(opcode, pending.binaryCaptureMask())
                        : null;
                case BARE_REFERENCE, UNARY -> captureBooleanStackValue(opcode);
                default -> null;
            };
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

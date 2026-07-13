package io.github.kd656.coveragex.core.instrument;

import io.github.kd656.coveragex.core.collect.CommonCoverageDataCollector;
import io.github.kd656.coveragex.api.data.ProbeMetadata;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.RecordComponentVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Injects coverage probes into class bytecode using ASM.
 *
 * <p>This is the fallback injector used when no source map ({@code coveragex.map.json})
 * is available for the class being instrumented. Condition text is derived from the
 * ASM branch opcode via {@link ProbeInjectionSupport#opcodeToConditionText(int)} and
 * produces generic placeholders such as {@code "if (x == null)"}.</p>
 *
 * <p>Probes are lightweight callback instructions inserted at strategic points
 * (method entries, branches) to record execution data at runtime. The entry
 * probe additionally captures the method name and boxed argument values.</p>
 *
 * <p>For each branch instruction two probes are injected — one for the TRUE
 * direction (jump taken) and one for the FALSE direction (fall-through) — so
 * that the report can distinguish which paths were exercised.</p>
 *
 * <p>Static {@link ProbeMetadata} is collected during instrumentation and
 * registered with the {@link CommonCoverageDataCollector} alongside the probe
 * hit array. This metadata flows through to the report layer unchanged.</p>
 *
 * <p>When a source map is available, prefer {@link SourceAwareProbeInjector} instead,
 * which populates {@code BranchProbe.conditionText} with verbatim source text.</p>
 */
public class DefaultProbeInjector implements ProbeInjector<byte[]> {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultProbeInjector.class);

    private final CommonCoverageDataCollector collector;

    public DefaultProbeInjector(CommonCoverageDataCollector collector) {
        this.collector = collector;
    }

    /**
     * Injects coverage probes into the given class bytecode.
     *
     * @param className  the internal class name (e.g. {@code org/example/Foo})
     * @param classBytes the original class file bytes
     * @return the instrumented class bytes
     */
    @Override
    public byte[] injectProbes(String className, byte[] classBytes) {
        LOG.trace("Injecting probes into: {}", className);

        var reader = new ClassReader(classBytes);
        var writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES);

        AtomicInteger probeCounter = new AtomicInteger(0);
        List<ProbeMetadata> metadataAccumulator = new ArrayList<>();

        var visitor = new ProbeInjectingClassVisitor(
                Opcodes.ASM9, writer, className, probeCounter, metadataAccumulator);

        reader.accept(visitor, ClassReader.EXPAND_FRAMES);

        int totalProbes = probeCounter.get();
        if (totalProbes > 0) {
            collector.registerClass(className, totalProbes, metadataAccumulator);
            LOG.debug("Instrumented {} with {} probes", className, totalProbes);
        }

        return writer.toByteArray();
    }

    // =========================================================================
    // ClassVisitor
    // =========================================================================

    private static class ProbeInjectingClassVisitor extends ClassVisitor {

        private final String className;
        private final AtomicInteger probeCounter;
        private final List<ProbeMetadata> metadataAccumulator;
        private boolean recordClass;
        private final Map<String, String> recordComponents = new HashMap<>();

        ProbeInjectingClassVisitor(int api, ClassVisitor cv, String className,
                                   AtomicInteger probeCounter,
                                   List<ProbeMetadata> metadataAccumulator) {
            super(api, cv);
            this.className = className;
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
                    || (recordClass && RecordMethods.isGeneratedObjectMethod(name, descriptor))
                    || (recordClass && RecordMethods.isComponentAccessorShape(name, descriptor, recordComponents))) {
                return mv;
            }

            return new DefaultProbeMethodVisitor(
                    api, mv, className, name, descriptor, access,
                    probeCounter, metadataAccumulator);
        }
    }

    // =========================================================================
    // MethodVisitor
    // =========================================================================

    /**
     * MethodVisitor that injects probe calls at method entry and branch points using
     * opcode-derived condition text. Extends {@link ProbeInjectionSupport} for all
     * shared instrumentation logic and accepts the default {@link #resolveConditionText}
     * implementation (opcode placeholder fallback).
     */
    private static class DefaultProbeMethodVisitor extends ProbeInjectionSupport {

        DefaultProbeMethodVisitor(int api, MethodVisitor mv, String className,
                                  String methodName, String descriptor, int access,
                                  AtomicInteger probeCounter,
                                  List<ProbeMetadata> metadataAccumulator) {
            super(api, mv, className, methodName, descriptor, access,
                    probeCounter, metadataAccumulator);
        }

        // No override of resolveConditionText — the base implementation
        // (opcodeToConditionText) is the correct behaviour for this injector.
    }
}

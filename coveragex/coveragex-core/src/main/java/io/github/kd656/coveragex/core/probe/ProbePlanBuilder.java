package io.github.kd656.coveragex.core.probe;

import io.github.kd656.coveragex.api.data.ProbeMetadata;
import io.github.kd656.coveragex.core.analysis.source.model.ClassModel;
import io.github.kd656.coveragex.core.analysis.source.model.MethodModel;
import io.github.kd656.coveragex.core.analysis.source.model.MethodReference;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Builds the static probe metadata model for a class without emitting transformed bytecode.
 *
 * <p>This is used by enrichment for classes that were never loaded during tests. It walks
 * bytecode with the same {@link ProbeMetadataVisitor} used by the runtime injector, but its
 * hook methods are intentionally empty because only metadata is needed.</p>
 */
public final class ProbePlanBuilder {

    /**
     * Convenience entry point for callers that only have raw class bytes.
     */
    public ProbePlan build(String expectedClassId, byte[] classBytes, ClassModel classModel) {
        return build(expectedClassId, new ClassReader(classBytes), classModel);
    }

    /**
     * Builds a probe plan from an existing reader so scanners can avoid parsing bytes twice.
     */
    public ProbePlan build(String expectedClassId, ClassReader reader, ClassModel classModel) {
        String classId = expectedClassId != null && !expectedClassId.isBlank()
                ? expectedClassId
                : reader.getClassName();

        AtomicInteger probeCounter = new AtomicInteger();
        List<ProbeMetadata> metadata = new ArrayList<>();

        reader.accept(new PlanningClassVisitor(
                Opcodes.ASM9, classModel, probeCounter, metadata), ClassReader.EXPAND_FRAMES);

        return new ProbePlan(classId, metadata);
    }

    private static final class PlanningClassVisitor extends ClassVisitor {

        private final ClassModel classModel;
        private final AtomicInteger probeCounter;
        private final List<ProbeMetadata> metadata;

        private PlanningClassVisitor(int api, ClassModel classModel,
                                     AtomicInteger probeCounter, List<ProbeMetadata> metadata) {
            super(api);
            this.classModel = classModel;
            this.probeCounter = probeCounter;
            this.metadata = metadata;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                         String signature, String[] exceptions) {
            if ((access & Opcodes.ACC_ABSTRACT) != 0) {
                return null;
            }

            MethodModel methodModel = classModel != null
                    ? classModel.getMethods().get(new MethodReference(name, descriptor))
                    : null;

            return new PlanningMethodVisitor(api, name, methodModel, probeCounter, metadata);
        }
    }

    private static final class PlanningMethodVisitor extends ProbeMetadataVisitor {

        private final MethodModel methodModel;
        private final SourceAwareBranchResolver branchResolver;
        private Boolean pendingJumpMeansTrue;

        private PlanningMethodVisitor(int api, String methodName, MethodModel methodModel,
                                      AtomicInteger probeCounter, List<ProbeMetadata> metadata) {
            super(api, null, methodName, probeCounter, metadata);
            this.methodModel = methodModel;
            this.branchResolver = new SourceAwareBranchResolver(methodModel);
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            if (methodModel != null) {
                // Prefer the source declaration line over javac's first executable line.
                methodStartLine = methodModel.getStartLine();
            }
            super.visitMaxs(maxStack, maxLocals);
        }

        @Override
        protected String resolveConditionText(int opcode, int branchLine) {
            SourceAwareBranchResolver.ResolvedBranch branch = branchResolver.resolve(opcode, branchLine);
            // The shared visitor asks for branch text before branch polarity; stash both from one lookup.
            pendingJumpMeansTrue = branch.jumpMeansTrue();
            return branch.conditionText();
        }

        @Override
        protected boolean isJumpTakenWhenTrue(int opcode) {
            if (pendingJumpMeansTrue != null) {
                boolean result = pendingJumpMeansTrue;
                pendingJumpMeansTrue = null;
                return result;
            }
            return super.isJumpTakenWhenTrue(opcode);
        }

        @Override
        protected void onMethodProbe(int probeId) {
        }

        @Override
        protected void onReturnProbe(int probeId) {
        }

        @Override
        protected void onThrowProbe(int probeId) {
        }

        @Override
        protected void onSegmentProbe(int probeId) {
        }

        @Override
        protected void onBranchProbe(int opcode, Label originalTarget,
                                     int fallThroughProbeId, int jumpTakenProbeId) {
        }
    }
}

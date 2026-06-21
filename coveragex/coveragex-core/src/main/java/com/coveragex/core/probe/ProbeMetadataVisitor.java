package com.coveragex.core.probe;

import com.coveragex.api.data.ProbeMetadata;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Shared ASM visitor that owns probe ID allocation and {@link ProbeMetadata} ordering.
 *
 * <p>The runtime agent and the static enrichment scanner both need identical metadata:
 * method probes, segment probes, branch TRUE/FALSE probes, return probes, and throw probes
 * must be assigned the same IDs for the same bytecode. This visitor centralizes that
 * structural logic and delegates side effects, such as bytecode emission, to hook methods.</p>
 */
public abstract class ProbeMetadataVisitor extends MethodVisitor {

    protected final String methodName;
    protected final AtomicInteger probeCounter;
    protected final List<ProbeMetadata> metadata;

    protected int currentLine = 0;
    protected int methodStartLine = -1;

    private int methodEntryProbeId = -1;
    private int segmentStartLine = -1;

    protected ProbeMetadataVisitor(int api, MethodVisitor delegate, String methodName,
                                   AtomicInteger probeCounter, List<ProbeMetadata> metadata) {
        super(api, delegate);
        this.methodName = methodName;
        this.probeCounter = probeCounter;
        this.metadata = metadata;
    }

    @Override
    public void visitCode() {
        super.visitCode();
        methodEntryProbeId = probeCounter.getAndIncrement();
        // The entry probe's end line is unknown until visitMaxs, so this slot is backfilled later.
        metadata.add(null);
        onMethodProbe(methodEntryProbeId);
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        currentLine = line;
        if (methodStartLine == -1) {
            methodStartLine = line;
        } else if (segmentStartLine == -1) {
            // The method declaration/entry line belongs to MethodProbe; segments start after it.
            segmentStartLine = line;
        }
        super.visitLineNumber(line, start);
    }

    @Override
    public void visitLabel(Label label) {
        finalizeSegmentAt(currentLine);
        super.visitLabel(label);
    }

    @Override
    public void visitJumpInsn(int opcode, Label target) {
        if (!ProbeOpcodeSupport.isBranchInstruction(opcode)) {
            super.visitJumpInsn(opcode, target);
            return;
        }

        finalizeSegmentAt(currentLine - 1);

        int branchLine = currentLine;
        String conditionText = resolveConditionText(opcode, branchLine);

        // IDs are first allocated by bytecode shape, then labelled by semantic branch direction.
        int fallThroughProbeId = probeCounter.getAndIncrement();
        int jumpTakenProbeId = probeCounter.getAndIncrement();

        // This reserves two slots in the metadata list at the same indexes as the probe IDs.
        metadata.add(null);
        metadata.add(null);

        boolean jumpMeansTrue = isJumpTakenWhenTrue(opcode);

        int trueProbeId = jumpMeansTrue ? jumpTakenProbeId : fallThroughProbeId;
        int falseProbeId = jumpMeansTrue ? fallThroughProbeId : jumpTakenProbeId;

        metadata.set(falseProbeId, new ProbeMetadata.BranchProbe(
                falseProbeId, methodName, branchLine, conditionText, ProbeMetadata.BranchDirection.FALSE));
        metadata.set(trueProbeId, new ProbeMetadata.BranchProbe(
                trueProbeId, methodName, branchLine, conditionText, ProbeMetadata.BranchDirection.TRUE));

        onBranchProbe(opcode, target, fallThroughProbeId, jumpTakenProbeId);
    }

    @Override
    public void visitInsn(int opcode) {
        if (ProbeOpcodeSupport.isThrow(opcode)) {
            // Throw lines get dedicated probes so the preceding segment does not swallow them.
            finalizeSegmentAt(currentLine - 1);
            int probeId = probeCounter.getAndIncrement();
            metadata.add(new ProbeMetadata.ThrowProbe(probeId, methodName, currentLine));
            onThrowProbe(probeId);
        } else if (ProbeOpcodeSupport.isReturn(opcode)) {
            // Return lines get dedicated probes so the preceding segment does not swallow them.
            finalizeSegmentAt(currentLine - 1);
            int probeId = probeCounter.getAndIncrement();
            metadata.add(new ProbeMetadata.ReturnProbe(probeId, methodName, currentLine));
            onReturnProbe(probeId);
        }
        super.visitInsn(opcode);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        if (methodEntryProbeId >= 0 && methodEntryProbeId < metadata.size()) {
            int startLine = methodStartLine == -1 ? 0 : methodStartLine;
            metadata.set(methodEntryProbeId,
                    new ProbeMetadata.MethodProbe(methodEntryProbeId, methodName, startLine, currentLine));
        }
        super.visitMaxs(maxStack, maxLocals);
    }

    protected String resolveConditionText(int opcode, int branchLine) {
        return ProbeOpcodeSupport.opcodeToConditionText(opcode);
    }

    protected boolean isJumpTakenWhenTrue(int opcode) {
        return ProbeOpcodeSupport.isJumpTakenWhenTrue(opcode);
    }

    protected final void emitJumpInsn(int opcode, Label label) {
        super.visitJumpInsn(opcode, label);
    }

    protected final void emitLabel(Label label) {
        super.visitLabel(label);
    }

    private void finalizeSegmentAt(int endLine) {
        if (segmentStartLine == -1) {
            return;
        }
        if (endLine >= segmentStartLine) {
            int probeId = probeCounter.getAndIncrement();
            metadata.add(new ProbeMetadata.SegmentProbe(probeId, methodName, segmentStartLine, endLine));
            onSegmentProbe(probeId);
        }
        segmentStartLine = -1;
    }

    protected abstract void onMethodProbe(int probeId);

    protected abstract void onReturnProbe(int probeId);

    protected abstract void onThrowProbe(int probeId);

    protected abstract void onSegmentProbe(int probeId);

    protected abstract void onBranchProbe(int opcode, Label originalTarget,
                                          int fallThroughProbeId, int jumpTakenProbeId);
}

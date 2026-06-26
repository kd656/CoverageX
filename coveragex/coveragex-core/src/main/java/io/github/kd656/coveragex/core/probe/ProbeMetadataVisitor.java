package io.github.kd656.coveragex.core.probe;

import io.github.kd656.coveragex.api.data.OperandKind;
import io.github.kd656.coveragex.api.data.ProbeMetadata;
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

    /**
     * Pending condition id for the branch currently being processed.
     * Set by {@link #resolveConditionText(int, int)} (or an override), consumed
     * in {@link #visitJumpInsn(int, Label)}, and reset to {@code -1} immediately
     * after use so it cannot bleed into the next instruction.
     */
    protected int pendingConditionId = -1;

    /**
     * Pending operand kind for the branch currently being processed.
     * Follows the same lifecycle as {@link #pendingConditionId}.
     */
    protected OperandKind pendingKind = OperandKind.UNKNOWN;

    /**
     * Pending arg labels for the branch currently being processed.
     * Follows the same lifecycle as {@link #pendingConditionId}.
     */
    protected List<String> pendingArgLabels = List.of();

    /**
     * Source-level parameter names for the method currently being visited.
     * Set by the source-aware override before the first {@code visitCode} call,
     * or left as an empty list when no source map is available. Consumed by
     * {@link #visitMaxs} to populate {@link ProbeMetadata.MethodProbe#parameterNames()}.
     */
    protected List<String> pendingParameterNames = List.of();

    /**
     * Local-variable slot indices holding the operand values stashed by the
     * capture emitter for the branch currently being processed. Each slot holds
     * a raw reference or a boxed primitive placed there directly by the capture
     * emitter; serialisation to {@link String} happens later in
     * {@link io.github.kd656.coveragex.core.collect.CommonCoverageDataCollector#attributeToTest}.
     * {@code null} when no operand capture was performed (UNKNOWN / UNARY / BARE_REFERENCE
     * operands, and the default path without a source map).
     *
     * <p>Consumed by {@link #visitJumpInsn} and forwarded to
     * {@link #onBranchProbe}, then reset to {@code null} so it cannot
     * bleed into the next instruction.</p>
     */
    protected int[] pendingOperandLocals = null;

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

        // Consume the pending operand metadata set by resolveConditionText (or its override)
        // and immediately reset to sentinels so they do not bleed into the next instruction.
        int condId = pendingConditionId;
        OperandKind kind = pendingKind;
        List<String> argLabels = pendingArgLabels;
        int[] operandLocals = pendingOperandLocals;
        pendingConditionId = -1;
        pendingKind = OperandKind.UNKNOWN;
        pendingArgLabels = List.of();
        pendingOperandLocals = null;

        metadata.set(falseProbeId, new ProbeMetadata.BranchProbe(
                falseProbeId, methodName, branchLine, conditionText,
                ProbeMetadata.BranchDirection.FALSE, condId, kind, argLabels));
        metadata.set(trueProbeId, new ProbeMetadata.BranchProbe(
                trueProbeId, methodName, branchLine, conditionText,
                ProbeMetadata.BranchDirection.TRUE, condId, kind, argLabels));

        onBranchProbe(opcode, target, fallThroughProbeId, jumpTakenProbeId, operandLocals);
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
                    new ProbeMetadata.MethodProbe(
                            methodEntryProbeId, methodName, startLine, currentLine,
                            pendingParameterNames));
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

    /**
     * Invoked when a conditional branch probe pair has been allocated. Subclasses
     * should emit or record the probe bytecode as appropriate.
     *
     * @param opcode             the original conditional-jump opcode
     * @param originalTarget     the original jump target label
     * @param fallThroughProbeId probe id for the fall-through direction
     * @param jumpTakenProbeId   probe id for the jump-taken direction
     * @param operandLocals      local-variable slot indices holding pre-stashed
     *                           operand values, or {@code null} when no capture
     *                           was performed for this operand
     */
    protected abstract void onBranchProbe(int opcode, Label originalTarget,
                                          int fallThroughProbeId, int jumpTakenProbeId,
                                          int[] operandLocals);
}

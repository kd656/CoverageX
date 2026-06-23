package io.github.kd656.coveragex.core.probe;

import io.github.kd656.coveragex.api.data.OperandKind;
import io.github.kd656.coveragex.core.analysis.source.model.DecisionModel;
import io.github.kd656.coveragex.core.analysis.source.model.MethodModel;
import io.github.kd656.coveragex.core.analysis.source.model.OperandModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves branch display text and TRUE/FALSE polarity from the source semantic model.
 *
 * <p>ASM exposes conditional jumps in bytecode order, while the source analyzer stores
 * operands by source line and operand index. This class bridges those two views and keeps
 * the per-line jump counter in one place so runtime instrumentation and static planning
 * consume source metadata identically.</p>
 */
public final class SourceAwareBranchResolver {

    private final Map<Integer, DecisionModel> decisionsByLine;
    private final Map<Integer, Integer> jumpCountPerLine = new HashMap<>();

    /**
     * Constructs a resolver backed by the given method model.
     *
     * @param methodModel the source model for the method being instrumented;
     *                    {@code null} when no source map is available, in which
     *                    case every call to {@link #resolve} returns the opcode fallback
     */
    public SourceAwareBranchResolver(MethodModel methodModel) {
        this.decisionsByLine = indexDecisions(methodModel);
    }

    /**
     * Resolves the next conditional jump on {@code branchLine}.
     *
     * <p>If source metadata is unavailable or incomplete, the resolver falls
     * back to opcode-derived text and returns sentinel values ({@code conditionId = -1},
     * {@code kind = UNKNOWN}, {@code argLabels = []}). Callers can then use the opcode
     * polarity table without special casing missing source maps.</p>
     *
     * @param opcode     the ASM conditional-jump opcode
     * @param branchLine the source line of the jump instruction
     * @return the resolved branch metadata, never {@code null}
     */
    public ResolvedBranch resolve(int opcode, int branchLine) {
        int jumpIndex = jumpCountPerLine.getOrDefault(branchLine, 0);
        jumpCountPerLine.put(branchLine, jumpIndex + 1);

        DecisionModel decision = decisionsByLine.get(branchLine);
        if (decision == null || jumpIndex >= decision.operands().size()) {
            return fallback(opcode);
        }

        OperandModel operand = decision.operands().get(jumpIndex);
        String conditionText = operand.conditionText();
        if (conditionText == null || conditionText.isBlank()) {
            return fallback(opcode);
        }

        return new ResolvedBranch(
                conditionText,
                operand.jumpMeansTrue(),
                operand.conditionId(),
                operand.kind(),
                operand.argLabels(),
                operand.binaryCaptureMask());
    }

    private static Map<Integer, DecisionModel> indexDecisions(MethodModel methodModel) {
        if (methodModel == null) {
            return Map.of();
        }

        Map<Integer, DecisionModel> result = new HashMap<>();
        for (DecisionModel decision : methodModel.getDecisionsList()) {
            // Preserve the previous "first decision on the line wins" behavior while avoiding per-jump scans.
            result.putIfAbsent(decision.conditionRange().beginLine(), decision);
        }
        return result;
    }

    /**
     * Returns the {@link OperandModel} for the next unconsumed conditional branch on
     * {@code line}, or {@code null} when no source metadata is available or the line
     * has no more operands.
     *
     * <p>This method is non-destructive: it does <em>not</em> advance the jump counter.
     * It is used by the capture emitter to inspect the upcoming operand's
     * {@link OperandModel#kind()} and {@link OperandModel#argLabels()} at
     * {@code visitMethodInsn} time, before the corresponding {@code visitJumpInsn}
     * fires and {@link #resolve} increments the counter.</p>
     *
     * @param line the source line of the instruction being inspected
     * @return the next pending operand, or {@code null}
     */
    public OperandModel peekNextOperand(int line) {
        DecisionModel decision = decisionsByLine.get(line);
        if (decision == null) {
            return null;
        }
        int jumpIndex = jumpCountPerLine.getOrDefault(line, 0);
        if (jumpIndex >= decision.operands().size()) {
            return null;
        }
        return decision.operands().get(jumpIndex);
    }

    private static ResolvedBranch fallback(int opcode) {
        return new ResolvedBranch(
                ProbeOpcodeSupport.opcodeToConditionText(opcode),
                null,
                -1,
                OperandKind.UNKNOWN,
                List.of(),
                0);
    }

    /**
     * The resolved metadata for a single conditional branch.
     *
     * @param conditionText      verbatim source text of the operand, or an opcode
     *                           placeholder when no source map is available
     * @param jumpMeansTrue      {@code true} when the bytecode jump fires on the
     *                           operand's {@code true} value; {@code null} when the
     *                           fallback path was taken and the opcode table should
     *                           be used
     * @param conditionId        1-based operand index within the parent decision;
     *                           {@code -1} on the fallback path
     * @param kind               structural classification of the operand;
     *                           {@link OperandKind#UNKNOWN} on the fallback path
     * @param argLabels          non-literal operand argument labels; empty on the
     *                           fallback path
     * @param binaryCaptureMask  for {@link OperandKind#BINARY_COMPARE} operands:
     *                           bit 0 set → lhs is capturable; bit 1 set → rhs is
     *                           capturable. {@code 0} for all other kinds and on
     *                           the fallback path.
     */
    public record ResolvedBranch(String conditionText,
                                 Boolean jumpMeansTrue,
                                 int conditionId,
                                 OperandKind kind,
                                 List<String> argLabels,
                                 int binaryCaptureMask) {
    }
}

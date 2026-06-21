package com.coveragex.core.probe;

import com.coveragex.core.analysis.source.model.DecisionModel;
import com.coveragex.core.analysis.source.model.MethodModel;
import com.coveragex.core.analysis.source.model.OperandModel;

import java.util.HashMap;
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

    public SourceAwareBranchResolver(MethodModel methodModel) {
        this.decisionsByLine = indexDecisions(methodModel);
    }

    /**
     * Resolves the next conditional jump on {@code branchLine}.
     *
     * <p>If source metadata is unavailable or incomplete, the resolver deliberately falls
     * back to opcode-derived text and leaves polarity unset. Callers can then use the
     * opcode polarity table without special casing missing source maps.</p>
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

        return new ResolvedBranch(conditionText, operand.jumpMeansTrue());
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

    private static ResolvedBranch fallback(int opcode) {
        return new ResolvedBranch(ProbeOpcodeSupport.opcodeToConditionText(opcode), null);
    }

    public record ResolvedBranch(String conditionText, Boolean jumpMeansTrue) {
    }
}

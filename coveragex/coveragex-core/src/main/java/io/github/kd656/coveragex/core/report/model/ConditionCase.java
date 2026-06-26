package io.github.kd656.coveragex.core.report.model;

import java.util.List;

/**
 * Coverage outcome for a single leaf operand within a decision. Mirrors
 * {@link io.github.kd656.coveragex.core.analysis.source.model.OperandModel}
 * on the analysis side; the two are paired by {@link #conditionId}.
 *
 * @param conditionId    1-based index of this operand within its parent
 *                       decision, stable across builds
 * @param conditionText  verbatim source text of the operand; may be
 *                       {@code null} for legacy map files
 * @param argLabels      non-literal operand argument labels, in source order,
 *                       used as column headers in the per-direction test table;
 *                       empty when no source map is available or the operand
 *                       produces no capturable arguments
 * @param trueDirection  outcome of the path taken when the operand
 *                       evaluates to {@code true}
 * @param falseDirection outcome of the path taken when the operand
 *                       evaluates to {@code false}
 */
public record ConditionCase(int conditionId,
                             String conditionText,
                             List<String> argLabels,
                             DirectionOutcome trueDirection,
                             DirectionOutcome falseDirection) {

    public ConditionCase {
        argLabels = List.copyOf(argLabels);
    }
}

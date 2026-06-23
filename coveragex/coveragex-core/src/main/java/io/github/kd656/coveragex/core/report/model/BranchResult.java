package io.github.kd656.coveragex.core.report.model;

import java.util.List;

/**
 * Coverage outcome for a single decision (one {@code if}, {@code while},
 * {@code for}, ternary or {@code assert} statement). Holds the per-operand
 * breakdown produced by the analyzer.
 *
 * @param methodName the enclosing method's simple name
 * @param line       the source line of the decision's first token
 * @param conditions one {@link ConditionCase} per leaf operand, in source
 *                   order
 */
public record BranchResult(String methodName,
                            int line,
                            List<ConditionCase> conditions) {

    /** Defensive copy so callers cannot mutate the list after construction. */
    public BranchResult {
        conditions = List.copyOf(conditions);
    }
}

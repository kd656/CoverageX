package io.github.kd656.coveragex.core.report;

import io.github.kd656.coveragex.api.data.ExecutionData;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs the existing {@link CoverageThresholdChecker} over one or more
 * {@link ReportInput}s according to the requested {@link ThresholdMode}.
 *
 * <p>Reuses the existing threshold math — this class only decides which snapshots to
 * check and how to package the outcomes.</p>
 */
public final class ThresholdEvaluator {

    private final CoverageThresholdChecker checker;

    public ThresholdEvaluator() {
        this(new CoverageThresholdChecker());
    }

    public ThresholdEvaluator(CoverageThresholdChecker checker) {
        this.checker = checker;
    }

    public ThresholdEvaluation evaluate(List<ReportInput> inputs,
                                         double minimumCoverage,
                                         ThresholdMode mode) {
        List<ScopedThresholdResult> results = new ArrayList<>();
        switch (mode) {
            case GLOBAL -> {
                List<ExecutionData> parts = new ArrayList<>(inputs.size());
                for (ReportInput input : inputs) {
                    parts.add(input.executionData());
                }
                ExecutionData merged = ExecutionData.merge(parts);
                results.add(ScopedThresholdResult.global(checker.check(merged, minimumCoverage)));
            }
            case PER_MODULE -> {
                for (ReportInput input : inputs) {
                    results.add(new ScopedThresholdResult(
                            input.scopeId(),
                            input.displayName(),
                            checker.check(input.executionData(), minimumCoverage)));
                }
            }
        }
        return new ThresholdEvaluation(mode, results);
    }
}

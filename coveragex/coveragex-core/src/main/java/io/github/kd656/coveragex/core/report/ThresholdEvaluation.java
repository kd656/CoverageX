package io.github.kd656.coveragex.core.report;

import java.util.ArrayList;
import java.util.List;

/**
 * Composite outcome of running the threshold check under a given {@link ThresholdMode}.
 *
 * <p>{@code GLOBAL} yields a single {@link ScopedThresholdResult#global result}; the
 * per-module mode yields one result per scope. {@link #passed()} is the conjunction —
 * every entry must pass for the whole evaluation to pass — and {@link #failures()} is
 * the subset the caller reports in a failure message.</p>
 */
public record ThresholdEvaluation(ThresholdMode mode, List<ScopedThresholdResult> results) {

    public ThresholdEvaluation {
        results = List.copyOf(results);
    }

    public boolean passed() {
        for (ScopedThresholdResult r : results) {
            if (!r.passed()) {
                return false;
            }
        }
        return true;
    }

    public List<ScopedThresholdResult> failures() {
        List<ScopedThresholdResult> failing = new ArrayList<>();
        for (ScopedThresholdResult r : results) {
            if (!r.passed()) {
                failing.add(r);
            }
        }
        return failing;
    }
}

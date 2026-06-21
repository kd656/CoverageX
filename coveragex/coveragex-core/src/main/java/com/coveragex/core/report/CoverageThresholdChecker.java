package com.coveragex.core.report;

import com.coveragex.api.data.ExecutionData;

import java.util.Objects;

/**
 * Checks whether overall probe coverage meets the configured minimum threshold.
 * <p>Returns a {@link ThresholdResult} so callers can decide whether to fail the build,
 * log a warning, or do nothing.</p>
 */
public final class CoverageThresholdChecker {

    public ThresholdResult check(ExecutionData data, double minimumCoverage) {
        Objects.requireNonNull(data, "data must not be null");
        double actualCoverage = data.probeCoveragePercent();
        return new ThresholdResult(actualCoverage, minimumCoverage, actualCoverage >= minimumCoverage);
    }

    public record ThresholdResult(double actualCoverage, double minimumCoverage, boolean passed) {}
}

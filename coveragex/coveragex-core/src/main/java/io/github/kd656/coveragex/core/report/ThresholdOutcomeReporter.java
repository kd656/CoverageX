package io.github.kd656.coveragex.core.report;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;

/**
 * Applies a {@link ThresholdEvaluation}: throws when the caller wants to fail
 * the build, otherwise logs a warning that names every failing scope.
 */
public final class ThresholdOutcomeReporter {

    private static final Logger LOG = LoggerFactory.getLogger(ThresholdOutcomeReporter.class);

    /**
     * @throws ThresholdViolationException if {@code failOnLowCoverage} is
     *         {@code true} and the evaluation has failures. Otherwise failures
     *         are logged and the method returns normally.
     */
    public void apply(ThresholdEvaluation evaluation, boolean failOnLowCoverage) {
        List<ScopedThresholdResult> failures = evaluation.failures();
        if (failures.isEmpty()) {
            if (hasNonZeroMinimum(evaluation)) {
                LOG.info("coveragex: coverage threshold met.");
            }
            return;
        }
        String failureMessage = formatFailureMessage(failures);
        if (failOnLowCoverage) {
            throw new ThresholdViolationException(failureMessage, evaluation);
        }
        LOG.warn(failureMessage);
        for (ScopedThresholdResult failing : failures) {
            LOG.warn(String.format(
                    Locale.ROOT,
                    "  %s: %.2f%% (below %.2f%%)",
                    labelOf(failing),
                    failing.threshold().actualCoverage(),
                    failing.threshold().minimumCoverage()));
        }
    }

    private String formatFailureMessage(List<ScopedThresholdResult> failures) {
        double minimum = failures.getFirst().threshold().minimumCoverage();
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.ROOT, "Coverage is below threshold %.2f%%:", minimum));
        for (ScopedThresholdResult failure : failures) {
            sb.append(System.lineSeparator())
                    .append("  ")
                    .append(labelOf(failure))
                    .append(": ")
                    .append(String.format(Locale.ROOT, "%.2f%%", failure.threshold().actualCoverage()));
        }
        return sb.toString();
    }

    private static String labelOf(ScopedThresholdResult result) {
        return result.displayName() != null && !result.displayName().isBlank()
                ? result.displayName()
                : result.scopeId();
    }

    private boolean hasNonZeroMinimum(ThresholdEvaluation evaluation) {
        for (ScopedThresholdResult r : evaluation.results()) {
            if (r.threshold().minimumCoverage() > 0.0) {
                return true;
            }
        }
        return false;
    }
}

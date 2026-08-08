package io.github.kd656.coveragex.core.report;

/**
 * Thrown when a {@link ThresholdEvaluation} contains failing scopes and the
 * caller has opted to fail on low coverage.
 */
public final class ThresholdViolationException extends RuntimeException {

    private final ThresholdEvaluation evaluation;

    public ThresholdViolationException(String message, ThresholdEvaluation evaluation) {
        super(message);
        this.evaluation = evaluation;
    }

    public ThresholdEvaluation evaluation() {
        return evaluation;
    }
}

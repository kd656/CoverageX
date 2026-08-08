package io.github.kd656.coveragex.core.report;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ThresholdOutcomeReporterTest {

    @Test
    void passingEvaluationDoesNotThrow() {
        assertThatCode(() -> new ThresholdOutcomeReporter().apply(passing(), true))
                .doesNotThrowAnyException();
    }

    @Test
    void failingEvaluationThrowsWhenFailOnLowCoverageIsTrue() {
        assertThatThrownBy(() -> new ThresholdOutcomeReporter().apply(failing(), true))
                .isInstanceOf(ThresholdViolationException.class)
                .hasMessageContaining("dto: 0.00%")
                .hasMessageContaining("Coverage is below threshold");
    }

    @Test
    void failingEvaluationDoesNotThrowWhenFailOnLowCoverageIsFalse() {
        assertThatCode(() -> new ThresholdOutcomeReporter().apply(failing(), false))
                .doesNotThrowAnyException();
    }

    @Test
    void exceptionCarriesEvaluation() {
        ThresholdEvaluation eval = failing();
        try {
            new ThresholdOutcomeReporter().apply(eval, true);
        } catch (ThresholdViolationException e) {
            assertThat(e.evaluation()).isSameAs(eval);
            return;
        }
        throw new AssertionError("expected ThresholdViolationException");
    }

    private static ThresholdEvaluation passing() {
        return new ThresholdEvaluation(ThresholdMode.PER_MODULE, List.of(
                new ScopedThresholdResult("dto", "dto",
                        new CoverageThresholdChecker.ThresholdResult(85.0, 80.0, true))));
    }

    private static ThresholdEvaluation failing() {
        return new ThresholdEvaluation(ThresholdMode.PER_MODULE, List.of(
                new ScopedThresholdResult("dto", "dto",
                        new CoverageThresholdChecker.ThresholdResult(0.0, 80.0, false)),
                new ScopedThresholdResult("service", "service",
                        new CoverageThresholdChecker.ThresholdResult(76.11, 80.0, false))));
    }
}

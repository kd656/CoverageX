package io.github.kd656.coveragex.core.report;

import io.github.kd656.coveragex.core.report.logic.ReportingService;

import java.util.List;

/**
 * The shared "render report, then gate on threshold" flow
 */
public final class CoverageReportRunner {

    private final ReportingService reportingService;
    private final ThresholdEvaluator thresholdEvaluator;
    private final ThresholdOutcomeReporter outcomeReporter;

    public CoverageReportRunner() {
        this(new ReportingService(), new ThresholdEvaluator(), new ThresholdOutcomeReporter());
    }

    CoverageReportRunner(ReportingService reportingService,
                         ThresholdEvaluator thresholdEvaluator,
                         ThresholdOutcomeReporter outcomeReporter) {
        this.reportingService = reportingService;
        this.thresholdEvaluator = thresholdEvaluator;
        this.outcomeReporter = outcomeReporter;
    }

    /** Renders every configured report format to disk. */
    public void render(ReportConfig config, List<ReportInput> inputs) {
        reportingService.report(config, inputs);
    }

    /**
     * Evaluates the coverage threshold across the inputs and applies the outcome.
     *
     * @throws ThresholdViolationException if {@code failOnLowCoverage} is {@code true}
     *         and at least one scope is below {@code minimumCoverage}
     */
    public void gate(List<ReportInput> inputs,
                     double minimumCoverage,
                     ThresholdMode mode,
                     boolean failOnLowCoverage) {
        ThresholdEvaluation evaluation = thresholdEvaluator.evaluate(inputs, minimumCoverage, mode);
        outcomeReporter.apply(evaluation, failOnLowCoverage);
    }
}

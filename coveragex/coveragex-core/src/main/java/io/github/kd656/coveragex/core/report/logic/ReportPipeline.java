package io.github.kd656.coveragex.core.report.logic;

import io.github.kd656.coveragex.core.report.ReportConfig;
import io.github.kd656.coveragex.core.report.model.ReportModel;
import io.github.kd656.coveragex.core.report.pipeline.ReportPipelineStep;
import io.github.kd656.coveragex.core.report.pipeline.steps.InsightsStep;
import io.github.kd656.coveragex.core.report.pipeline.steps.InvocationTrackingStep;
import io.github.kd656.coveragex.core.report.pipeline.steps.MCDCStep;
import io.github.kd656.coveragex.core.report.pipeline.steps.OverCoverageStep;
import io.github.kd656.coveragex.core.report.pipeline.steps.SuggestionsStep;
import io.github.kd656.coveragex.core.report.pipeline.steps.TestTrackingStep;

import java.util.List;

/**
 * Runs the enabled {@link ReportPipelineStep}s over a {@link ReportModel}.
 *
 * <p>Extracted from {@code ReportingService} so aggregated reporting can reuse the
 * same chain. A fresh instance is safe to run repeatedly — steps are constructed
 * per invocation, so their {@code next} references do not leak across runs.</p>
 */
public final class ReportPipeline {

    public void run(ReportConfig config, ReportModel model) {
        List<ReportPipelineStep> all = List.of(
                new InvocationTrackingStep(),
                new TestTrackingStep(),
                new InsightsStep(),
                new SuggestionsStep(),
                new MCDCStep(),
                new OverCoverageStep()
        );

        List<ReportPipelineStep> enabled = all.stream()
                .filter(step -> config.isStepEnabled(step.stepId()))
                .toList();

        for (int i = 0; i < enabled.size() - 1; i++) {
            enabled.get(i).setNext(enabled.get(i + 1));
        }

        if (!enabled.isEmpty()) {
            enabled.getFirst().handle(model);
        }
    }
}

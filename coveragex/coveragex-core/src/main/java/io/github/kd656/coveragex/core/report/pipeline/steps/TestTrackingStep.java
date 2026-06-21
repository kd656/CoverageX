package io.github.kd656.coveragex.core.report.pipeline.steps;

import io.github.kd656.coveragex.api.data.ClassTestCoverage;
import io.github.kd656.coveragex.api.data.TestTrackingSnapshot;
import io.github.kd656.coveragex.core.report.model.ClassMetrics;
import io.github.kd656.coveragex.core.report.model.ReportModel;
import io.github.kd656.coveragex.core.report.pipeline.PipelineStepId;
import io.github.kd656.coveragex.core.report.pipeline.ReportPipelineStep;

import java.util.LinkedHashMap;
import java.util.Map;

public class TestTrackingStep extends ReportPipelineStep {

    @Override
    public PipelineStepId stepId() {
        return PipelineStepId.TEST_TRACKING;
    }

    @Override
    public void handle(ReportModel model) {
        Map<String, ClassTestCoverage> classes = new LinkedHashMap<>();

        for (ClassMetrics cm : model.getClassMetrics()) {
            ClassTestCoverage attribution = cm.testAttribution();
            if (!attribution.probeInvocations().isEmpty()) {
                classes.put(cm.classId(), attribution);
            }
        }

        model.setTestTrackingSnapshot(new TestTrackingSnapshot(Map.copyOf(classes)));
        proceed(model);
    }
}

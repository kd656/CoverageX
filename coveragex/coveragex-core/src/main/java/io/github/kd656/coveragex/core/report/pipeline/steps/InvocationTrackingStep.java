package io.github.kd656.coveragex.core.report.pipeline.steps;

import io.github.kd656.coveragex.core.report.model.ClassMetrics;
import io.github.kd656.coveragex.core.report.model.MethodMetrics;
import io.github.kd656.coveragex.core.report.model.ReportModel;
import io.github.kd656.coveragex.core.report.pipeline.PipelineStepId;
import io.github.kd656.coveragex.core.report.pipeline.ReportPipelineStep;
import io.github.kd656.coveragex.core.report.pipeline.results.InvocationReport;
import io.github.kd656.coveragex.core.report.pipeline.results.InvocationResult;

import java.util.ArrayList;
import java.util.List;

public class InvocationTrackingStep extends ReportPipelineStep {

    @Override
    public PipelineStepId stepId() {
        return PipelineStepId.INVOCATION_TRACKING;
    }

    @Override
    public void handle(ReportModel model) {
        List<InvocationReport> reports = new ArrayList<>();

        for (ClassMetrics classMetrics : model.getClassMetrics()) {
            for (MethodMetrics methodMetrics : classMetrics.methods()) {
                if (methodMetrics.isImplicitDefaultConstructor() || methodMetrics.invocations().isEmpty()) {
                    continue;
                }
                reports.add(
                        new InvocationReport(classMetrics.classId(),
                            methodMetrics.methodName(),
                            methodMetrics.startLine(),
                            methodMetrics.hitCount(),
                            List.copyOf(methodMetrics.invocations())
                ));
            }
        }

        model.put(InvocationResult.class, new InvocationResult(List.copyOf(reports)));
        proceed(model);
    }
}

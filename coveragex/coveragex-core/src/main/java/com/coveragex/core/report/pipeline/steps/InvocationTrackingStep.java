package com.coveragex.core.report.pipeline.steps;

import com.coveragex.core.report.model.ClassMetrics;
import com.coveragex.core.report.model.MethodMetrics;
import com.coveragex.core.report.model.ReportModel;
import com.coveragex.core.report.pipeline.PipelineStepId;
import com.coveragex.core.report.pipeline.ReportPipelineStep;
import com.coveragex.core.report.pipeline.results.InvocationReport;
import com.coveragex.core.report.pipeline.results.InvocationResult;

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

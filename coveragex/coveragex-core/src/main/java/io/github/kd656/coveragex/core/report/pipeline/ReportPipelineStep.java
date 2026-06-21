package io.github.kd656.coveragex.core.report.pipeline;

import io.github.kd656.coveragex.core.report.model.ReportModel;

public abstract class ReportPipelineStep {

    private ReportPipelineStep next;

    public final ReportPipelineStep setNext(ReportPipelineStep next) {
        this.next = next;
        return next;
    }

    public abstract PipelineStepId stepId();

    public abstract void handle(ReportModel model);

    protected final void proceed(ReportModel model) {
        if (next != null) {
            next.handle(model);
        }
    }
}

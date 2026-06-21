package com.coveragex.core.report.pipeline.steps;

import com.coveragex.core.report.model.ReportModel;
import com.coveragex.core.report.pipeline.PipelineStepId;
import com.coveragex.core.report.pipeline.ReportPipelineStep;
import com.coveragex.core.report.pipeline.results.MCDCResult;

import java.util.List;

/**
 * MC/DC analysis step.
 * Not yet implemented — true MC/DC requires per-condition independence tracking
 * at the instrumentation level, which is not currently recorded.
 */
public class MCDCStep extends ReportPipelineStep {

    @Override
    public PipelineStepId stepId() {
        return PipelineStepId.MCDC;
    }

    @Override
    public void handle(ReportModel model) {
        // TODO: Implement true MC/DC analysis when per-condition tracking is added to instrumentation.
        model.put(MCDCResult.class, new MCDCResult(List.of()));
        proceed(model);
    }
}

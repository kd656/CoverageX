package com.coveragex.core.report.pipeline.steps;

import com.coveragex.core.report.model.ClassMetrics;
import com.coveragex.core.report.model.MethodMetrics;
import com.coveragex.core.report.model.ReportModel;
import com.coveragex.core.report.pipeline.PipelineStepId;
import com.coveragex.core.report.pipeline.ReportPipelineStep;
import com.coveragex.core.report.pipeline.results.OverCoverageResult;
import com.coveragex.core.report.pipeline.results.OverCoverageWarning;

import java.util.ArrayList;
import java.util.List;

public class OverCoverageStep extends ReportPipelineStep {

    private static final int HIGH_CALL_THRESHOLD = 50;
    private static final int TRIVIAL_CALL_THRESHOLD = 20;

    @Override
    public PipelineStepId stepId() {
        return PipelineStepId.OVER_COVERAGE;
    }

    @Override
    public void handle(ReportModel model) {
        List<OverCoverageWarning> warnings = new ArrayList<>();

        for (ClassMetrics cm : model.getClassMetrics()) {
            for (MethodMetrics mm : cm.methods()) {
                if (mm.isImplicitDefaultConstructor()) {
                    continue;
                }
                int totalInvocations = mm.hitCount();
                int uniqueCombos = mm.invocations().size();
                boolean fullCoverage = mm.probeCount() > 0 && mm.hitProbeCount() == mm.probeCount();

                if (fullCoverage && totalInvocations > HIGH_CALL_THRESHOLD && uniqueCombos <= 2) {
                    warnings.add(new OverCoverageWarning(cm.classId(), mm.methodName(), mm.startLine(),
                        // TODO: String.format to be used.
                        "Heavily tested with low argument diversity (" + totalInvocations
                            + " calls, " + uniqueCombos + " unique combinations)."));
                }

                if (mm.probeCount() == 1 && totalInvocations > TRIVIAL_CALL_THRESHOLD) {
                    // TODO: String.format to be used.
                    warnings.add(new OverCoverageWarning(cm.classId(), mm.methodName(), mm.startLine(),
                        "Trivial " + (mm.isConstructor() ? "isConstructor" : "method") + " (1 probe) invoked " + totalInvocations
                            + " times — testing effort likely excessive."));
                }
            }
        }

        model.put(OverCoverageResult.class, new OverCoverageResult(List.copyOf(warnings)));
        proceed(model);
    }
}

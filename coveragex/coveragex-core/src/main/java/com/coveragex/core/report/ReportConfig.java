package com.coveragex.core.report;

import com.coveragex.core.report.model.ReportingType;
import com.coveragex.core.report.pipeline.PipelineStepId;

import java.util.Set;

public record ReportConfig(
    Set<ReportingType> reportFormats,
    boolean enableInvocationTracking,
    boolean enableInsights,
    boolean enableSuggestions,
    boolean enableMCDC,
    boolean enableOverCoverageAnalysis,
    double minimumCoverage,
    boolean failOnLowCoverage,
    ReportContext context
) {
    public boolean isStepEnabled(PipelineStepId stepId) {
        return switch (stepId) {
            case BASE_METRICS          -> true;
            case INVOCATION_TRACKING   -> enableInvocationTracking;
            case TEST_TRACKING         -> enableInvocationTracking;
            case INSIGHTS              -> enableInsights;
            case SUGGESTIONS           -> enableSuggestions;
            case MCDC                  -> enableMCDC;
            case OVER_COVERAGE         -> enableOverCoverageAnalysis;
        };
    }
}

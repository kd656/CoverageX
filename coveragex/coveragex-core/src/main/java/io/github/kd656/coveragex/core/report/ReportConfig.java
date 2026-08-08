package io.github.kd656.coveragex.core.report;

import io.github.kd656.coveragex.core.report.model.ReportingType;
import io.github.kd656.coveragex.core.report.pipeline.PipelineStepId;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;

public record ReportConfig(
    Set<ReportingType> reportFormats,
    boolean enableInvocationTracking,
    boolean enableInsights,
    boolean enableSuggestions,
    boolean enableMCDC,
    boolean enableOverCoverageAnalysis,
    double minimumCoverage,
    ReportContext context
) {

    public static ReportConfig of(
            Path outputDir,
            Path sourceDir,
            Collection<String> reportFormats,
            boolean enableInvocationTracking,
            boolean enableInsights,
            boolean enableSuggestions,
            boolean enableMCDC,
            boolean enableOverCoverageAnalysis,
            double minimumCoverage) {
        return new ReportConfig(
                ReportingTypeParser.parse(reportFormats),
                enableInvocationTracking,
                enableInsights,
                enableSuggestions,
                enableMCDC,
                enableOverCoverageAnalysis,
                minimumCoverage,
                new ReportContext(outputDir, sourceDir));
    }

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

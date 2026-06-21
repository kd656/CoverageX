package io.github.kd656.coveragex.core.report;

import io.github.kd656.coveragex.api.data.ExecutionData;
import io.github.kd656.coveragex.api.io.internal.BinaryDataReader;
import io.github.kd656.coveragex.core.report.logic.ReportingService;
import io.github.kd656.coveragex.core.report.model.ReportingType;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;

public final class ReportPipelineRunner {

    public record Config(
        Path execFile,
        Collection<String> formats,
        Path outputDir,
        Path sourceDir,
        double minimumCoverage,
        boolean failOnLowCoverage,
        boolean enableInvocationTracking,
        boolean enableInsights,
        boolean enableSuggestions,
        boolean enableMCDC,
        boolean enableOverCoverageAnalysis
    ) {}

    public CoverageThresholdChecker.ThresholdResult run(Config config) throws IOException {
        ExecutionData data = new BinaryDataReader().read(config.execFile());
        Set<ReportingType> types = ReportingTypeParser.parse(config.formats());
        ReportContext context = new ReportContext(config.outputDir(), config.sourceDir());
        ReportConfig reportConfig = new ReportConfig(
            types,
            config.enableInvocationTracking(),
            config.enableInsights(),
            config.enableSuggestions(),
            config.enableMCDC(),
            config.enableOverCoverageAnalysis(),
            config.minimumCoverage(),
            config.failOnLowCoverage(),
            context
        );
        new ReportingService().report(reportConfig, data);
        return new CoverageThresholdChecker().check(data, config.minimumCoverage());
    }
}

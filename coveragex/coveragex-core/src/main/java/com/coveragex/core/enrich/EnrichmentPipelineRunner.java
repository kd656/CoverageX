package com.coveragex.core.enrich;

import com.coveragex.api.data.ExecutionData;
import com.coveragex.api.io.internal.BinaryDataReader;
import com.coveragex.api.io.internal.BinaryDataWriter;
import com.coveragex.core.analysis.source.CoverageContextResolver;
import com.coveragex.core.scan.ClassCoverageFilter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class EnrichmentPipelineRunner {

    public record Config(
        Path execFile,
        List<Path> classDirectories,
        Path mapFile,
        List<String> includes,
        List<String> excludes
    ) {}

    public record Result(int includedClassCount, int addedClassCount) {}

    public Result run(Config config) throws IOException {
        ExecutionData data = new BinaryDataReader().read(config.execFile());
        ClassCoverageFilter filter = new ClassCoverageFilter(config.includes(), config.excludes());
        CoverageContextResolver resolver = new CoverageContextResolver(config.mapFile());
        CoverageDataEnrichmentService service = new CoverageDataEnrichmentService();

        int included = 0, added = 0;
        ExecutionData current = data;
        for (Path dir : config.classDirectories()) {
            CoverageDataEnrichmentService.EnrichmentResult r =
                service.enrich(current, dir, filter, resolver);
            current = r.executionData();
            included += r.includedClassCount();
            added += r.addedClassCount();
        }
        new BinaryDataWriter().write(config.execFile(), current);
        return new Result(included, added);
    }
}

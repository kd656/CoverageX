package io.github.kd656.coveragex.core.enrich;

import io.github.kd656.coveragex.api.data.ClassCoverage;
import io.github.kd656.coveragex.api.data.ExecutionData;
import io.github.kd656.coveragex.core.analysis.source.CoverageContextResolver;
import io.github.kd656.coveragex.core.probe.ProbePlan;
import io.github.kd656.coveragex.core.scan.ClassCoverageFilter;
import io.github.kd656.coveragex.core.scan.ClassFileScanner;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Merges zero-coverage entries for production classes that the agent never instrumented.
 * <p>Scans a compiled classes directory and adds any class absent from the runtime
 * {@link io.github.kd656.coveragex.api.data.ExecutionData} with all probes set to zero, so reports
 * reflect genuinely untested code rather than simply omitting it.</p>
 */
public final class CoverageDataEnrichmentService {

    private final ClassFileScanner classFileScanner;

    public CoverageDataEnrichmentService() {
        this(new ClassFileScanner());
    }

    public CoverageDataEnrichmentService(ClassFileScanner classFileScanner) {
        this.classFileScanner = classFileScanner;
    }

    public EnrichmentResult enrich(ExecutionData runtimeData,
                                   Path classesDir,
                                   ClassCoverageFilter filter,
                                   CoverageContextResolver coverageContextResolver) throws IOException {
        Map<String, ProbePlan> allClasses = classFileScanner.scan(classesDir, filter, coverageContextResolver);
        Map<String, ClassCoverage> merged = new LinkedHashMap<>(runtimeData.classes());

        int added = 0;
        for (Map.Entry<String, ProbePlan> entry : allClasses.entrySet()) {
            ProbePlan plan = entry.getValue();
            if (!merged.containsKey(entry.getKey())) {
                merged.put(entry.getKey(),
                        ClassCoverage.zeroCoverage(plan.classId(), plan.metadata()));
                added++;
            }
        }

        return new EnrichmentResult(new ExecutionData(merged), allClasses.size(), added);
    }

    public record EnrichmentResult(ExecutionData executionData, int includedClassCount, int addedClassCount) {
    }
}

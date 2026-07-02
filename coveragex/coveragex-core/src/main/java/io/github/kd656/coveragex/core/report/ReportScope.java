package io.github.kd656.coveragex.core.report;

import io.github.kd656.coveragex.core.report.model.ClassMetrics;
import io.github.kd656.coveragex.core.report.model.SummaryMetrics;

import java.nio.file.Path;
import java.util.List;

/**
 * Per-module slice of a scoped {@link io.github.kd656.coveragex.core.report.model.ReportModel}.
 *
 * <p>Carries the computed metrics for one scope so views can render module
 * groupings without re-running the model factory per module. Single-module
 * reports have exactly one scope with a synthetic {@code "main"} id.</p>
 */
public record ReportScope(
        String scopeId,
        String displayName,
        Path sourceDirectory,
        List<ClassMetrics> classMetrics,
        SummaryMetrics summaryMetrics
) {
    public ReportScope {
        classMetrics = List.copyOf(classMetrics);
    }
}

package io.github.kd656.coveragex.core.report.model;

public record SummaryMetrics(
    int totalProbes,
    int executedProbes,
    double lineCoveragePercent,
    int classCount
) {}

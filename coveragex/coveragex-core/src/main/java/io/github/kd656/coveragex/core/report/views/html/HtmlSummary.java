package io.github.kd656.coveragex.core.report.views.html;

public record HtmlSummary(
    String timestamp,
    double coveragePct,
    int classCount,
    int totalProbes,
    int executedProbes,
    int notCoveredProbes,
    long critCount, long warnCount, long infoCount, long posCount
) {}

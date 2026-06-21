package io.github.kd656.coveragex.core.report.pipeline.results;

public record OverCoverageWarning(String classId, String methodName, int line, String reason) {}

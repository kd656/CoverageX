package io.github.kd656.coveragex.core.report.pipeline.results;

import io.github.kd656.coveragex.api.data.InvocationRecord;
import java.util.List;

public record InvocationReport(
    String classId,
    String methodName,
    int startLine,
    int totalCallCount,
    List<InvocationRecord> argCombinations
) {}

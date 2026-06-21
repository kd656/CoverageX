package com.coveragex.core.report.pipeline.results;

import com.coveragex.api.data.InvocationRecord;
import java.util.List;

public record InvocationReport(
    String classId,
    String methodName,
    int startLine,
    int totalCallCount,
    List<InvocationRecord> argCombinations
) {}

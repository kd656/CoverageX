package com.coveragex.core.report.model;

import com.coveragex.api.data.InvocationRecord;
import com.coveragex.api.data.ProbeMetadata;
import java.util.List;

public record MethodMetrics(
    String methodName,
    boolean isConstructor,
    boolean isImplicitDefaultConstructor,
    int startLine,
    int endLine,
    int hitCount,
    int probeCount,
    int hitProbeCount,
    int branchProbeCount,
    int hitBranchProbeCount,
    List<InvocationRecord> invocations
) {
    public String methodKey() {
        return methodKey(methodName, startLine);
    }

    public static String methodKey(String methodName, int startLine) {
        return methodName + ":" + startLine;
    }

    public static String methodKey(ProbeMetadata.MethodProbe probe) {
        return methodKey(probe.methodName(), probe.startLine());
    }
}

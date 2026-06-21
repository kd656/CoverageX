package com.coveragex.api.data;

import java.util.List;

public record MethodHit(String methodName, List<InvocationRecord> invocations) {
    public MethodHit {
        invocations = List.copyOf(invocations);
    }
}

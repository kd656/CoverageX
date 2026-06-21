package com.coveragex.api.data;

import java.util.List;
import java.util.Map;

public record ClassTestCoverage(String classId, Map<Integer, List<AttributedInvocation>> probeInvocations) {
    public ClassTestCoverage {
        probeInvocations = Map.copyOf(probeInvocations);
    }

    public static ClassTestCoverage empty(String classId) {
        return new ClassTestCoverage(classId, Map.of());
    }
}

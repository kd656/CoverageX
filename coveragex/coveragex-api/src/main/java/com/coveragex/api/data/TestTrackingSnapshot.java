package com.coveragex.api.data;

import java.util.Map;

public record TestTrackingSnapshot(
    Map<String, ClassTestCoverage> classCoverages
) {
    public static TestTrackingSnapshot empty() {
        return new TestTrackingSnapshot(Map.of());
    }

    public ClassTestCoverage forClass(String classId) {
        return classCoverages.getOrDefault(classId, ClassTestCoverage.empty(classId));
    }
}

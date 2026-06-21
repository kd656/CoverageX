package io.github.kd656.coveragex.api.data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ExecutionData(
        Map<String, ClassCoverage> classes,
        int totalProbes,
        int executedProbes
) {
    public ExecutionData(List<ClassCoverage> classes) {
        this(toMapByClassId(classes));
    }

    public ExecutionData(Map<String, ClassCoverage> classes) {
        this(
            Map.copyOf(classes),
            computeTotalProbes(classes),
            computeExecutedProbes(classes)
        );
    }

    public double probeCoveragePercent() {
        return totalProbes > 0 ? (100.0 * executedProbes / totalProbes) : 0.0;
    }

    public int classCount() {
        return classes.size();
    }

    public ClassCoverage classCoverage(String classId) {
        return classes.get(classId);
    }

    private static Map<String, ClassCoverage> toMapByClassId(List<ClassCoverage> classes) {
        Map<String, ClassCoverage> map = new LinkedHashMap<>();
        for (ClassCoverage cc : classes) {
            map.put(cc.classId(), cc);
        }
        return map;
    }

    private static int computeTotalProbes(Map<String, ClassCoverage> classes) {
        int total = 0;
        for (ClassCoverage cc : classes.values()) {
            total += cc.probeHits().length;
        }
        return total;
    }

    private static int computeExecutedProbes(Map<String, ClassCoverage> classes) {
        int executed = 0;
        for (ClassCoverage cc : classes.values()) {
            for (boolean hit : cc.probeHits()) {
                if (hit) executed++;
            }
        }
        return executed;
    }
}

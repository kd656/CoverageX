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

    /**
     * Flat union of multiple execution-data snapshots.
     *
     * <p>Used by the multi-module aggregator's GLOBAL threshold path, where the
     * input has already been routed so each FQCN belongs to a single owner scope.
     * If the same FQCN turns up in two parts anyway, throws
     * {@link DuplicateClassCoverageException} naming both contributor indices —
     * a real build problem worth surfacing (e.g. code generation emitting the
     * same class into two modules' {@code target/classes}).</p>
     *
     * <p>Callers that intend to combine hits on the same FQCN (the routing
     * scenario) should use {@link ClassCoverage#merge} on the individual
     * coverages instead; this method's contract is "one owner per FQCN".</p>
     */
    public static ExecutionData merge(List<ExecutionData> parts) {
        if (parts == null || parts.isEmpty()) {
            throw new IllegalArgumentException("parts must not be empty");
        }
        Map<String, ClassCoverage> merged = new LinkedHashMap<>();
        Map<String, String> ownerLabelByClassId = new java.util.HashMap<>();
        for (int idx = 0; idx < parts.size(); idx++) {
            String partLabel = "part[" + idx + "]";
            ExecutionData part = parts.get(idx);
            for (Map.Entry<String, ClassCoverage> e : part.classes().entrySet()) {
                String classId = e.getKey();
                if (merged.containsKey(classId)) {
                    throw new DuplicateClassCoverageException(
                            classId,
                            ownerLabelByClassId.get(classId),
                            partLabel);
                }
                merged.put(classId, e.getValue());
                ownerLabelByClassId.put(classId, partLabel);
            }
        }
        return new ExecutionData(merged);
    }
}


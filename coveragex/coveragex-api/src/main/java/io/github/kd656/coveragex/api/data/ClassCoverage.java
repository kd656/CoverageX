package io.github.kd656.coveragex.api.data;


import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record ClassCoverage(
        String classId,
        boolean[] probeHits,
        Map<Integer, MethodHit> methodHits,
        Map<Integer, ProbeHit> hits,
        List<ProbeMetadata> probeMetadata,
        ClassTestCoverage testAttribution
) {
    public ClassCoverage {
        Objects.requireNonNull(classId, "classId");
        Objects.requireNonNull(probeHits, "probeHits");
        probeHits = Arrays.copyOf(probeHits, probeHits.length);
        methodHits = Map.copyOf(Objects.requireNonNull(methodHits, "methodHits"));
        hits = Map.copyOf(Objects.requireNonNull(hits, "hits"));
        probeMetadata = probeMetadata != null ? List.copyOf(probeMetadata) : List.of();
        testAttribution = testAttribution != null ? testAttribution : ClassTestCoverage.empty(classId);
    }

    @Override
    public boolean[] probeHits() {
        return Arrays.copyOf(probeHits, probeHits.length);
    }

    public boolean isProbeHit(int probeId) {
        return probeId >= 0 && probeId < probeHits.length && probeHits[probeId];
    }

    public static ClassCoverage zeroCoverage(String classId, List<ProbeMetadata> metadata) {
        List<ProbeMetadata> safeMetadata = metadata != null ? metadata : List.of();
        return new ClassCoverage(
                classId,
                new boolean[safeMetadata.size()],
                Map.of(),
                Map.of(),
                safeMetadata,
                ClassTestCoverage.empty(classId)
        );
    }

    /**
     * Merges two coverages for the same FQCN.
     *
     * <p>Used by the multi-module aggregator: when the same class is exercised by
     * tests in two modules, both {@code ClassCoverage} records land in the owner
     * module's bucket and are collapsed here.</p>
     *
     * <p>Per-field semantics:</p>
     * <ul>
     *   <li>{@code probeHits}: bitwise OR over equal-length arrays. Length divergence
     *       throws — indicates the same FQCN was instrumented from two different
     *       bytecode versions (e.g. mixed compile targets).</li>
     *   <li>{@code methodHits}: per-probeId merge via {@link MethodHit#merge}.</li>
     *   <li>{@code hits}: per-probeId merge via {@link ProbeHit#merge}.</li>
     *   <li>{@code probeMetadata}: prefer the non-empty side (identical for the
     *       same FQCN in normal builds).</li>
     *   <li>{@code testAttribution}: union via {@link ClassTestCoverage#merge}.</li>
     * </ul>
     */
    public static ClassCoverage merge(ClassCoverage a, ClassCoverage b) {
        if (!a.classId().equals(b.classId())) {
            throw new IllegalArgumentException(
                    "classId mismatch: " + a.classId() + " vs " + b.classId());
        }

        if (a.probeHits.length == 0) {
            return b;
        }

        if (b.probeHits.length == 0) {
            return a;
        }

        boolean[] mergedProbeHits = mergeProbeHits(a, b);
        Map<Integer, MethodHit> mergedMethodHits = mergeMethodHits(a.methodHits(), b.methodHits());
        Map<Integer, ProbeHit> mergedHits = mergeHits(a.hits(), b.hits());
        List<ProbeMetadata> mergedMetadata = preferNonEmpty(a.probeMetadata(), b.probeMetadata());
        ClassTestCoverage mergedAttribution =
                ClassTestCoverage.merge(a.testAttribution(), b.testAttribution());
        return new ClassCoverage(
                a.classId(),
                mergedProbeHits,
                mergedMethodHits,
                mergedHits,
                mergedMetadata,
                mergedAttribution
        );
    }

    private static boolean[] mergeProbeHits(ClassCoverage a, ClassCoverage b) {
        int lenA = a.probeHits.length;
        int lenB = b.probeHits.length;
        if (lenA != lenB) {
            throw new IllegalArgumentException(
                    "probeHits length divergence for " + a.classId() +
                    ": " + lenA + " vs " + lenB +
                    " (same FQCN instrumented from different bytecode versions?)");
        }
        boolean[] out = new boolean[lenA];
        for (int i = 0; i < lenA; i++) {
            out[i] = a.probeHits[i] || b.probeHits[i];
        }
        return out;
    }

    private static Map<Integer, MethodHit> mergeMethodHits(Map<Integer, MethodHit> a,
                                                            Map<Integer, MethodHit> b) {
        Map<Integer, MethodHit> merged = new LinkedHashMap<>(a);
        for (Map.Entry<Integer, MethodHit> e : b.entrySet()) {
            merged.compute(e.getKey(), (k, existing) ->
                    existing == null ? e.getValue() : MethodHit.merge(existing, e.getValue()));
        }
        return merged;
    }

    private static Map<Integer, ProbeHit> mergeHits(Map<Integer, ProbeHit> a,
                                                     Map<Integer, ProbeHit> b) {
        Map<Integer, ProbeHit> merged = new LinkedHashMap<>(a);
        for (Map.Entry<Integer, ProbeHit> e : b.entrySet()) {
            merged.compute(e.getKey(), (k, existing) ->
                    existing == null ? e.getValue() : ProbeHit.merge(existing, e.getValue()));
        }
        return merged;
    }

    private static List<ProbeMetadata> preferNonEmpty(List<ProbeMetadata> a, List<ProbeMetadata> b) {
        if (a.isEmpty()) {
            return b;
        }
        return a;
    }
}


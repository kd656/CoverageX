package io.github.kd656.coveragex.api.data;


import java.util.Arrays;
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
}

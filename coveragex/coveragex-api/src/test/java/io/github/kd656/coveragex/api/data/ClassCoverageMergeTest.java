package io.github.kd656.coveragex.api.data;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClassCoverageMergeTest {

    @Test
    void mergeOrsProbeHitsOfEqualLength() {
        ClassCoverage a = coverageWith(new boolean[]{true, false, false, true});
        ClassCoverage b = coverageWith(new boolean[]{false, true, false, true});

        ClassCoverage merged = ClassCoverage.merge(a, b);

        assertThat(merged.probeHits()).containsExactly(true, true, false, true);
    }

    @Test
    void mergePreservesClassId() {
        ClassCoverage a = coverageWith(new boolean[]{true});
        ClassCoverage b = coverageWith(new boolean[]{true});

        assertThat(ClassCoverage.merge(a, b).classId()).isEqualTo("example/Foo");
    }

    @Test
    void mergeThrowsOnProbeHitsLengthDivergence() {
        ClassCoverage a = coverageWith(new boolean[]{true, false});
        ClassCoverage b = coverageWith(new boolean[]{true, false, true});

        assertThatThrownBy(() -> ClassCoverage.merge(a, b))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("probeHits length divergence")
            .hasMessageContaining("example/Foo");
    }

    @Test
    void mergeCombinesMethodHitsPerProbeId() {
        ClassCoverage a = new ClassCoverage(
            "example/Foo",
            new boolean[]{true},
            Map.of(0, new MethodHit("greet", List.of(new InvocationRecord(List.of("alice"), 2)))),
            Map.of(),
            List.of(),
            null
        );
        ClassCoverage b = new ClassCoverage(
            "example/Foo",
            new boolean[]{true},
            Map.of(0, new MethodHit("greet", List.of(new InvocationRecord(List.of("bob"), 3)))),
            Map.of(),
            List.of(),
            null
        );

        ClassCoverage merged = ClassCoverage.merge(a, b);

        MethodHit method = merged.methodHits().get(0);
        assertThat(method.methodName()).isEqualTo("greet");
        assertThat(method.invocations())
            .extracting(InvocationRecord::args, InvocationRecord::count)
            .containsExactly(
                org.assertj.core.api.Assertions.tuple(List.of("alice"), 2),
                org.assertj.core.api.Assertions.tuple(List.of("bob"), 3)
            );
    }

    @Test
    void mergeSumsProbeHitCountsPerProbeId() {
        ClassCoverage a = new ClassCoverage(
            "example/Foo",
            new boolean[]{true, true},
            Map.of(),
            Map.of(0, new ProbeHit(0, 3), 1, new ProbeHit(1, 1)),
            List.of(),
            null
        );
        ClassCoverage b = new ClassCoverage(
            "example/Foo",
            new boolean[]{true, false},
            Map.of(),
            Map.of(0, new ProbeHit(0, 5)),
            List.of(),
            null
        );

        ClassCoverage merged = ClassCoverage.merge(a, b);

        assertThat(merged.hits().get(0).count()).isEqualTo(8);
        assertThat(merged.hits().get(1).count()).isEqualTo(1);
    }

    @Test
    void mergePrefersNonEmptyProbeMetadata() {
        List<ProbeMetadata> metadata = List.of(
            new ProbeMetadata.MethodProbe(0, "f", 1, 5, List.of())
        );
        ClassCoverage a = new ClassCoverage(
            "example/Foo",
            new boolean[]{true},
            Map.of(),
            Map.of(),
            List.of(),
            null
        );
        ClassCoverage b = new ClassCoverage(
            "example/Foo",
            new boolean[]{true},
            Map.of(),
            Map.of(),
            metadata,
            null
        );

        assertThat(ClassCoverage.merge(a, b).probeMetadata()).isEqualTo(metadata);
        assertThat(ClassCoverage.merge(b, a).probeMetadata()).isEqualTo(metadata);
    }

    @Test
    void mergeDelegatesTestAttributionMerge() {
        ClassCoverage a = new ClassCoverage(
            "example/Foo",
            new boolean[]{true},
            Map.of(),
            Map.of(),
            List.of(),
            new ClassTestCoverage("example/Foo", Map.of(
                0, List.of(new AttributedInvocation(List.of("x"), List.of("testA")))
            ))
        );
        ClassCoverage b = new ClassCoverage(
            "example/Foo",
            new boolean[]{true},
            Map.of(),
            Map.of(),
            List.of(),
            new ClassTestCoverage("example/Foo", Map.of(
                0, List.of(new AttributedInvocation(List.of("x"), List.of("testB")))
            ))
        );

        ClassCoverage merged = ClassCoverage.merge(a, b);

        assertThat(merged.testAttribution().probeInvocations().get(0).getFirst().testMethods())
            .containsExactly("testA", "testB");
    }

    @Test
    void mergeRejectsMismatchedClassIds() {
        ClassCoverage a = coverageWith(new boolean[]{true});
        ClassCoverage b = new ClassCoverage("example/Bar", new boolean[]{true}, Map.of(), Map.of(), List.of(), null);
        assertThatThrownBy(() -> ClassCoverage.merge(a, b))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("classId mismatch");
    }

    private static ClassCoverage coverageWith(boolean[] probeHits) {
        return new ClassCoverage(
            "example/Foo",
            probeHits,
            Map.of(),
            Map.of(),
            List.of(),
            null
        );
    }
}

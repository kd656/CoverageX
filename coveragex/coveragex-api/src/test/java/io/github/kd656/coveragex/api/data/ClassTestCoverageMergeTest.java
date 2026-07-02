package io.github.kd656.coveragex.api.data;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClassTestCoverageMergeTest {

    @Test
    void mergeUnionsInvocationsPerProbe() {
        ClassTestCoverage a = new ClassTestCoverage("example/Foo", Map.of(
            0, List.of(new AttributedInvocation(List.of("x"), List.of("testA")))
        ));
        ClassTestCoverage b = new ClassTestCoverage("example/Foo", Map.of(
            0, List.of(new AttributedInvocation(List.of("y"), List.of("testB")))
        ));

        ClassTestCoverage merged = ClassTestCoverage.merge(a, b);

        List<AttributedInvocation> probeZero = merged.probeInvocations().get(0);
        assertThat(probeZero)
            .extracting(AttributedInvocation::args, AttributedInvocation::testMethods)
            .containsExactly(
                org.assertj.core.api.Assertions.tuple(List.of("x"), List.of("testA")),
                org.assertj.core.api.Assertions.tuple(List.of("y"), List.of("testB"))
            );
    }

    @Test
    void mergeUnionsTestMethodsWhenArgsMatch() {
        ClassTestCoverage a = new ClassTestCoverage("example/Foo", Map.of(
            0, List.of(new AttributedInvocation(List.of("x"), List.of("testA")))
        ));
        ClassTestCoverage b = new ClassTestCoverage("example/Foo", Map.of(
            0, List.of(new AttributedInvocation(List.of("x"), List.of("testB")))
        ));

        ClassTestCoverage merged = ClassTestCoverage.merge(a, b);

        List<AttributedInvocation> probeZero = merged.probeInvocations().get(0);
        assertThat(probeZero).hasSize(1);
        assertThat(probeZero.getFirst().args()).containsExactly("x");
        assertThat(probeZero.getFirst().testMethods()).containsExactly("testA", "testB");
    }

    @Test
    void mergeDeduplicatesRepeatedTestMethodsOnUnion() {
        ClassTestCoverage a = new ClassTestCoverage("example/Foo", Map.of(
            0, List.of(new AttributedInvocation(List.of("x"), List.of("testA")))
        ));
        ClassTestCoverage b = new ClassTestCoverage("example/Foo", Map.of(
            0, List.of(new AttributedInvocation(List.of("x"), List.of("testA")))
        ));

        ClassTestCoverage merged = ClassTestCoverage.merge(a, b);

        assertThat(merged.probeInvocations().get(0).getFirst().testMethods())
            .containsExactly("testA");
    }

    @Test
    void mergePreservesProbesPresentInOnlyOneSide() {
        ClassTestCoverage a = new ClassTestCoverage("example/Foo", Map.of(
            0, List.of(new AttributedInvocation(List.of("x"), List.of("testA")))
        ));
        ClassTestCoverage b = new ClassTestCoverage("example/Foo", Map.of(
            1, List.of(new AttributedInvocation(List.of("y"), List.of("testB")))
        ));

        ClassTestCoverage merged = ClassTestCoverage.merge(a, b);

        assertThat(merged.probeInvocations()).containsOnlyKeys(0, 1);
        assertThat(merged.probeInvocations().get(0).getFirst().testMethods()).containsExactly("testA");
        assertThat(merged.probeInvocations().get(1).getFirst().testMethods()).containsExactly("testB");
    }

    @Test
    void mergeRejectsMismatchedClassIds() {
        ClassTestCoverage a = ClassTestCoverage.empty("example/Foo");
        ClassTestCoverage b = ClassTestCoverage.empty("example/Bar");
        assertThatThrownBy(() -> ClassTestCoverage.merge(a, b))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("classId mismatch");
    }
}

package io.github.kd656.coveragex.api.data;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MethodHitMergeTest {

    @Test
    void mergeConcatenatesDisjointInvocations() {
        MethodHit a = new MethodHit("greet", List.of(new InvocationRecord(List.of("alice"), 2)));
        MethodHit b = new MethodHit("greet", List.of(new InvocationRecord(List.of("bob"), 3)));

        MethodHit merged = MethodHit.merge(a, b);

        assertThat(merged.methodName()).isEqualTo("greet");
        assertThat(merged.invocations())
            .extracting(InvocationRecord::args, InvocationRecord::count)
            .containsExactly(
                org.assertj.core.api.Assertions.tuple(List.of("alice"), 2),
                org.assertj.core.api.Assertions.tuple(List.of("bob"), 3)
            );
    }

    @Test
    void mergeSumsCountsForIdenticalArgs() {
        MethodHit a = new MethodHit("greet", List.of(new InvocationRecord(List.of("alice"), 2)));
        MethodHit b = new MethodHit("greet", List.of(new InvocationRecord(List.of("alice"), 5)));

        MethodHit merged = MethodHit.merge(a, b);

        assertThat(merged.invocations()).hasSize(1);
        InvocationRecord only = merged.invocations().getFirst();
        assertThat(only.args()).containsExactly("alice");
        assertThat(only.count()).isEqualTo(7);
    }

    @Test
    void mergeCollapsesMixedDuplicatesAndUniques() {
        MethodHit a = new MethodHit("f", List.of(
            new InvocationRecord(List.of("x"), 1),
            new InvocationRecord(List.of("y"), 2)
        ));
        MethodHit b = new MethodHit("f", List.of(
            new InvocationRecord(List.of("y"), 3),
            new InvocationRecord(List.of("z"), 4)
        ));

        MethodHit merged = MethodHit.merge(a, b);

        assertThat(merged.invocations())
            .extracting(InvocationRecord::args, InvocationRecord::count)
            .containsExactly(
                org.assertj.core.api.Assertions.tuple(List.of("x"), 1),
                org.assertj.core.api.Assertions.tuple(List.of("y"), 5),
                org.assertj.core.api.Assertions.tuple(List.of("z"), 4)
            );
    }

    @Test
    void mergeRejectsMismatchedMethodNames() {
        MethodHit a = new MethodHit("greet", List.of());
        MethodHit b = new MethodHit("farewell", List.of());
        assertThatThrownBy(() -> MethodHit.merge(a, b))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("methodName mismatch");
    }
}

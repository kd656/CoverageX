package io.github.kd656.coveragex.api.data;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExecutionDataMergeTest {

    @Test
    void mergeUnionsDisjointClasses() {
        ExecutionData a = new ExecutionData(Map.of(
            "example/A", coverage("example/A", new boolean[]{true, false})
        ));
        ExecutionData b = new ExecutionData(Map.of(
            "example/B", coverage("example/B", new boolean[]{true, true})
        ));

        ExecutionData merged = ExecutionData.merge(List.of(a, b));

        assertThat(merged.classes()).containsOnlyKeys("example/A", "example/B");
        assertThat(merged.totalProbes()).isEqualTo(4);
        assertThat(merged.executedProbes()).isEqualTo(3);
    }

    @Test
    void mergeIncludesEveryClassFromEveryPart() {
        // ExecutionData wraps its map in Map.copyOf which does not guarantee
        // iteration order, so this test asserts membership only. Callers that
        // need deterministic iteration should build a LinkedHashMap themselves.
        LinkedHashMap<String, ClassCoverage> partA = new LinkedHashMap<>();
        partA.put("example/Z", coverage("example/Z", new boolean[]{true}));
        LinkedHashMap<String, ClassCoverage> partB = new LinkedHashMap<>();
        partB.put("example/A", coverage("example/A", new boolean[]{true}));

        ExecutionData merged = ExecutionData.merge(List.of(
            new ExecutionData(partA),
            new ExecutionData(partB)
        ));

        assertThat(merged.classes().keySet()).containsExactlyInAnyOrder("example/Z", "example/A");
    }

    @Test
    void mergeThrowsOnDuplicateClassId() {
        ExecutionData a = new ExecutionData(Map.of(
            "example/Foo", coverage("example/Foo", new boolean[]{true})
        ));
        ExecutionData b = new ExecutionData(Map.of(
            "example/Foo", coverage("example/Foo", new boolean[]{true})
        ));

        assertThatThrownBy(() -> ExecutionData.merge(List.of(a, b)))
            .isInstanceOf(DuplicateClassCoverageException.class)
            .hasMessageContaining("example/Foo")
            .hasMessageContaining("part[0]")
            .hasMessageContaining("part[1]");
    }

    @Test
    void duplicateExceptionCarriesOwnerLabels() {
        ExecutionData a = new ExecutionData(Map.of(
            "example/Foo", coverage("example/Foo", new boolean[]{true})
        ));
        ExecutionData b = new ExecutionData(Map.of(
            "example/Foo", coverage("example/Foo", new boolean[]{true})
        ));

        try {
            ExecutionData.merge(List.of(a, b));
            org.assertj.core.api.Assertions.fail("expected DuplicateClassCoverageException");
        } catch (DuplicateClassCoverageException ex) {
            assertThat(ex.classId()).isEqualTo("example/Foo");
            assertThat(ex.ownerA()).isEqualTo("part[0]");
            assertThat(ex.ownerB()).isEqualTo("part[1]");
        }
    }

    @Test
    void mergeRejectsEmptyList() {
        assertThatThrownBy(() -> ExecutionData.merge(List.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("empty");
    }

    @Test
    void mergeRejectsNullList() {
        assertThatThrownBy(() -> ExecutionData.merge(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void mergeSingleInputProducesEquivalentSnapshot() {
        ClassCoverage cc = coverage("example/A", new boolean[]{true, false, true});
        ExecutionData original = new ExecutionData(Map.of("example/A", cc));

        ExecutionData merged = ExecutionData.merge(List.of(original));

        assertThat(merged.classes()).containsOnlyKeys("example/A");
        assertThat(merged.totalProbes()).isEqualTo(3);
        assertThat(merged.executedProbes()).isEqualTo(2);
    }

    private static ClassCoverage coverage(String classId, boolean[] probeHits) {
        return new ClassCoverage(classId, probeHits, Map.of(), Map.of(), List.of(), null);
    }
}

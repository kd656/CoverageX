package io.github.kd656.coveragex.core.report;

import io.github.kd656.coveragex.api.data.ClassCoverage;
import io.github.kd656.coveragex.api.data.DuplicateClassCoverageException;
import io.github.kd656.coveragex.api.data.ExecutionData;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ThresholdEvaluatorTest {

    @Test
    void globalModeMergesAllInputsIntoOneCheck() {
        ReportInput dto     = input("dto",     "dto/A", new boolean[]{true, true});      // 100%
        ReportInput service = input("service", "service/B", new boolean[]{true, false}); // 50%

        ThresholdEvaluation eval = new ThresholdEvaluator()
            .evaluate(List.of(dto, service), 60.0, ThresholdMode.GLOBAL);

        assertThat(eval.results()).hasSize(1);
        ScopedThresholdResult only = eval.results().getFirst();
        assertThat(only.scopeId()).isEqualTo("global");
        // Merged: 3 executed / 4 total = 75%. Above 60% → passed.
        assertThat(only.threshold().actualCoverage()).isEqualTo(75.0);
        assertThat(only.passed()).isTrue();
        assertThat(eval.passed()).isTrue();
    }

    @Test
    void globalModeSurfacesDuplicateClassConflicts() {
        // Both inputs contain the same FQCN post-routing → real ownership conflict.
        ReportInput a = input("a", "shared/Class", new boolean[]{true});
        ReportInput b = input("b", "shared/Class", new boolean[]{true});

        assertThatThrownBy(() -> new ThresholdEvaluator()
            .evaluate(List.of(a, b), 50.0, ThresholdMode.GLOBAL))
            .isInstanceOf(DuplicateClassCoverageException.class);
    }

    @Test
    void perModuleChecksEveryInputIndependently() {
        ReportInput dto     = input("dto",     "dto/A",     new boolean[]{true, false}); // 50%
        ReportInput service = input("service", "service/B", new boolean[]{true, true});  // 100%

        ThresholdEvaluation eval = new ThresholdEvaluator()
            .evaluate(List.of(dto, service), 80.0, ThresholdMode.PER_MODULE);

        assertThat(eval.results()).hasSize(2);
        assertThat(eval.results())
            .extracting(ScopedThresholdResult::scopeId, ScopedThresholdResult::passed)
            .containsExactly(
                org.assertj.core.api.Assertions.tuple("dto", false),
                org.assertj.core.api.Assertions.tuple("service", true));
        assertThat(eval.passed()).isFalse();
        assertThat(eval.failures())
            .singleElement()
            .extracting(ScopedThresholdResult::scopeId)
            .isEqualTo("dto");
    }

    @Test
    void perModulePassesWhenEveryModuleMeetsThreshold() {
        ReportInput dto     = input("dto",     "dto/A",     new boolean[]{true, true});
        ReportInput service = input("service", "service/B", new boolean[]{true, true});

        ThresholdEvaluation eval = new ThresholdEvaluator()
            .evaluate(List.of(dto, service), 100.0, ThresholdMode.PER_MODULE);

        assertThat(eval.passed()).isTrue();
        assertThat(eval.failures()).isEmpty();
    }

    @Test
    void perModuleReportsFailuresInReactorOrder() {
        // dto fails; service passes; api fails. Expected failures list preserves order.
        ReportInput dto     = input("dto",     "dto/A",     new boolean[]{false});
        ReportInput service = input("service", "service/B", new boolean[]{true});
        ReportInput api     = input("api",     "api/C",     new boolean[]{false});

        ThresholdEvaluation eval = new ThresholdEvaluator()
            .evaluate(List.of(dto, service, api), 50.0, ThresholdMode.PER_MODULE);

        assertThat(eval.failures())
            .extracting(ScopedThresholdResult::scopeId)
            .containsExactly("dto", "api");
    }

    private static ReportInput input(String scopeId, String classId, boolean[] probeHits) {
        ClassCoverage cc = new ClassCoverage(classId, probeHits, Map.of(), Map.of(), List.of(), null);
        return new ReportInput(scopeId, scopeId, null, new ExecutionData(Map.of(classId, cc)));
    }
}

package io.github.kd656.coveragex.core.report.logic;

import io.github.kd656.coveragex.api.data.ClassCoverage;
import io.github.kd656.coveragex.api.data.ClassTestCoverage;
import io.github.kd656.coveragex.api.data.ExecutionData;
import io.github.kd656.coveragex.api.data.InvocationRecord;
import io.github.kd656.coveragex.api.data.MethodHit;
import io.github.kd656.coveragex.api.data.ProbeHit;
import io.github.kd656.coveragex.api.data.ProbeMetadata;
import io.github.kd656.coveragex.core.report.model.BranchResult;
import io.github.kd656.coveragex.core.report.model.ClassMetrics;
import io.github.kd656.coveragex.core.report.model.MethodMetrics;
import io.github.kd656.coveragex.core.report.model.ReportModel;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ReportingServiceTest {

    @Test
    void assignsOutOfOrderProbesToOverloadBySourceRange() throws Exception {
        List<ProbeMetadata> metadata = List.of(
            new ProbeMetadata.MethodProbe(0, "normalize", 10, 15),
            new ProbeMetadata.BranchProbe(2, "normalize", 22, "value == null", ProbeMetadata.BranchDirection.TRUE),
            new ProbeMetadata.MethodProbe(1, "normalize", 20, 25),
            new ProbeMetadata.SegmentProbe(3, "normalize", 23, 24)
        );
        boolean[] probeHits = { true, true, true, false };
        Map<Integer, MethodHit> entryProbes = Map.of(
            0, new MethodHit("normalize", List.of(
                new InvocationRecord(List.of("text"), 2)
            )),
            1, new MethodHit("normalize", List.of(
                new InvocationRecord(List.of("1"), 3)
            ))
        );
        ExecutionData data = new ExecutionData(List.of(
            new ClassCoverage("example/OverloadedService", probeHits, entryProbes, Map.of(), metadata, null)
        ));

        ReportModel model = buildInitialModel(data);
        ClassMetrics classMetrics = model.getClassMetrics().getFirst();

        assertThat(classMetrics.methods())
            .extracting(MethodMetrics::methodKey)
            .containsExactly("normalize:10", "normalize:20");

        MethodMetrics firstOverload = classMetrics.methods().get(0);
        MethodMetrics secondOverload = classMetrics.methods().get(1);

        assertThat(firstOverload.hitCount()).isEqualTo(2);
        assertThat(firstOverload.branchProbeCount()).isZero();

        assertThat(secondOverload.hitCount()).isEqualTo(3);
        assertThat(secondOverload.branchProbeCount()).isEqualTo(1);
        assertThat(secondOverload.probeCount()).isEqualTo(3);
    }

    @Test
    void branchResultsCarryRealInvocationCountsFromHitsMap() throws Exception {
        // Loop body executed 7 times → TRUE direction count = 7;
        // Loop exit taken once → FALSE direction count = 1.
        List<ProbeMetadata> metadata = List.of(
            new ProbeMetadata.MethodProbe(0, "loop", 10, 20),
            new ProbeMetadata.BranchProbe(1, "loop", 12, "i < n", ProbeMetadata.BranchDirection.TRUE),
            new ProbeMetadata.BranchProbe(2, "loop", 12, "i < n", ProbeMetadata.BranchDirection.FALSE)
        );
        boolean[] probeHits = { true, true, true };
        Map<Integer, ProbeHit> hits = Map.of(
            0, new ProbeHit(0, 1),
            1, new ProbeHit(1, 7),
            2, new ProbeHit(2, 1)
        );
        ExecutionData data = new ExecutionData(List.of(
            new ClassCoverage("example/Loop", probeHits, Map.of(), hits, metadata, ClassTestCoverage.empty("example/Loop"))
        ));

        ReportModel model = buildInitialModel(data);
        List<BranchResult> branches = model.getClassMetrics().getFirst().branches();

        assertThat(branches).hasSize(1);
        BranchResult only = branches.getFirst();
        assertThat(only.trueHit()).isTrue();
        assertThat(only.falseHit()).isTrue();
        assertThat(only.trueCount()).isEqualTo(7);
        assertThat(only.falseCount()).isEqualTo(1);
    }

    private ReportModel buildInitialModel(ExecutionData data) throws Exception {
        Method method = ReportingService.class.getDeclaredMethod("buildInitialModel", ExecutionData.class);
        method.setAccessible(true);
        return (ReportModel) method.invoke(new ReportingService(), data);
    }
}

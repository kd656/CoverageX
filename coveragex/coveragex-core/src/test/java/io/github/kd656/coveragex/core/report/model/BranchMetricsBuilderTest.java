package io.github.kd656.coveragex.core.report.model;

import io.github.kd656.coveragex.api.data.OperandKind;
import io.github.kd656.coveragex.api.data.ProbeHit;
import io.github.kd656.coveragex.api.data.ProbeMetadata.BranchDirection;
import io.github.kd656.coveragex.api.data.ProbeMetadata.BranchProbe;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pinning behaviour of {@link BranchMetricsBuilder}, in particular the
 * fallback pairing path when no source map is present.
 */
class BranchMetricsBuilderTest {

    @Test
    void pairsByExplicitConditionIdWhenSourceMapIsPresent() {
        BranchProbe t1 = branch(0, "m", 10, "a", BranchDirection.TRUE,  1);
        BranchProbe f1 = branch(1, "m", 10, "a", BranchDirection.FALSE, 1);
        BranchProbe t2 = branch(2, "m", 10, "b", BranchDirection.TRUE,  2);
        BranchProbe f2 = branch(3, "m", 10, "b", BranchDirection.FALSE, 2);

        List<BranchResult> results = BranchMetricsBuilder.build(
                List.of(t1, f1, t2, f2),
                new boolean[]{true, false, false, true},
                Map.of(0, new ProbeHit(0, 1), 3, new ProbeHit(3, 1)));

        assertThat(results).hasSize(1);
        BranchResult br = results.get(0);
        assertThat(br.conditions()).extracting(ConditionCase::conditionId).containsExactly(1, 2);
        assertThat(br.conditions().get(0).trueDirection().hit()).isTrue();
        assertThat(br.conditions().get(0).falseDirection().hit()).isFalse();
        assertThat(br.conditions().get(1).trueDirection().hit()).isFalse();
        assertThat(br.conditions().get(1).falseDirection().hit()).isTrue();
    }

    @Test
    void fallsBackToPositionalPairingWhenSourceMapMissing() {
        // Two compound operands, all probes carry conditionId = -1 (no source map).
        BranchProbe t1 = branch(0, "m", 10, "?", BranchDirection.TRUE,  -1);
        BranchProbe f1 = branch(1, "m", 10, "?", BranchDirection.FALSE, -1);
        BranchProbe t2 = branch(2, "m", 10, "?", BranchDirection.TRUE,  -1);
        BranchProbe f2 = branch(3, "m", 10, "?", BranchDirection.FALSE, -1);

        List<BranchResult> results = BranchMetricsBuilder.build(
                List.of(t1, f1, t2, f2),
                new boolean[]{true, false, false, true},
                Map.of(0, new ProbeHit(0, 1), 3, new ProbeHit(3, 1)));

        assertThat(results).hasSize(1);
        BranchResult br = results.get(0);
        assertThat(br.conditions())
                .as("two operands must survive the source-map-less fallback")
                .hasSize(2);
        assertThat(br.conditions().get(0).trueDirection().hit()).isTrue();
        assertThat(br.conditions().get(0).falseDirection().hit()).isFalse();
        assertThat(br.conditions().get(1).trueDirection().hit()).isFalse();
        assertThat(br.conditions().get(1).falseDirection().hit()).isTrue();
    }

    private static BranchProbe branch(int probeId, String method, int line,
                                       String text, BranchDirection direction,
                                       int conditionId) {
        return new BranchProbe(
                probeId, method, line, text, direction,
                conditionId, OperandKind.UNKNOWN, List.of());
    }
}

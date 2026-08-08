package io.github.kd656.coveragex.api.data;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProbeHitTest {

    @Test
    void zeroCountIsNotAHit() {
        ProbeHit hit = new ProbeHit(7, 0);
        assertThat(hit.wasHit()).isFalse();
        assertThat(hit.count()).isZero();
        assertThat(hit.probeId()).isEqualTo(7);
    }

    @Test
    void positiveCountIsAHit() {
        ProbeHit hit = new ProbeHit(3, 5);
        assertThat(hit.wasHit()).isTrue();
        assertThat(hit.count()).isEqualTo(5);
    }

    @Test
    void negativeCountRejected() {
        assertThatThrownBy(() -> new ProbeHit(1, -1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("count must be >= 0");
    }

    @Test
    void mergeSumsCountsForMatchingProbeId() {
        ProbeHit merged = ProbeHit.merge(new ProbeHit(5, 3), new ProbeHit(5, 7));
        assertThat(merged.probeId()).isEqualTo(5);
        assertThat(merged.count()).isEqualTo(10);
    }

    @Test
    void mergePreservesZeroCounts() {
        ProbeHit merged = ProbeHit.merge(new ProbeHit(2, 0), new ProbeHit(2, 0));
        assertThat(merged.wasHit()).isFalse();
    }

    @Test
    void mergeRejectsMismatchedProbeIds() {
        assertThatThrownBy(() -> ProbeHit.merge(new ProbeHit(1, 1), new ProbeHit(2, 1)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("probeId mismatch");
    }
}


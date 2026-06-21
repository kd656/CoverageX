package com.coveragex.api.data;

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
}

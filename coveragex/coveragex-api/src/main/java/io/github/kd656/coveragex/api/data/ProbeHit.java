package io.github.kd656.coveragex.api.data;

/**
 * Counter for a single probe. {@code count} is the number of times the
 * probe fired during the recorded run; {@link #wasHit()} is the cheap
 * boolean projection.
 */
public record ProbeHit(int probeId, int count) {

    public ProbeHit {
        if (count < 0) {
            throw new IllegalArgumentException("count must be >= 0, was " + count);
        }
    }

    public boolean wasHit() {
        return count > 0;
    }
}

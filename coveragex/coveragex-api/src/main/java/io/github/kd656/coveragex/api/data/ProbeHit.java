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

    /**
     * Merges two hits for the same probe by summing their counts.
     *
     * <p>Used by {@link ClassCoverage#merge} when the same FQCN is exercised
     * from multiple modules and gets rebucketed into the owning module.</p>
     */
    public static ProbeHit merge(ProbeHit a, ProbeHit b) {
        if (a.probeId() != b.probeId()) {
            throw new IllegalArgumentException(
                    "probeId mismatch: " + a.probeId() + " vs " + b.probeId());
        }
        return new ProbeHit(a.probeId(), a.count() + b.count());
    }
}


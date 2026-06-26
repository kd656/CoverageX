package io.github.kd656.coveragex.core.instrument.stubs;

/**
 * Stub type used by method-call capture tests.
 * Accepts a {@code long} argument so that category-2 spilling can be exercised.
 */
public final class LongLimits {

    private final long max;

    /**
     * Constructs a limits instance with the given maximum.
     *
     * @param max the maximum allowed value (inclusive)
     */
    public LongLimits(long max) {
        this.max = max;
    }

    /**
     * Returns {@code true} when {@code value} is within the allowed range.
     *
     * @param value the long value to check
     * @return {@code true} if {@code value <= max}
     */
    public boolean isAllowed(long value) {
        return value <= max;
    }

    @Override
    public String toString() {
        return "L{}";
    }
}

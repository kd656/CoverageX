package org.example.branches;

/**
 * Stub limits type for {@link BranchCaptureShapes} demonstration fixtures.
 * Accepts a {@code long} value so that the category-2 spill path is exercised.
 */
public final class Limits {

    private final long max;

    /**
     * Constructs a limits instance with the given upper bound.
     *
     * @param max the maximum allowed value (inclusive)
     */
    public Limits(long max) {
        this.max = max;
    }

    /**
     * Returns {@code true} when {@code value} does not exceed the limit.
     *
     * @param value the long value to check
     * @return {@code true} if {@code value <= max}
     */
    public boolean isAllowed(long value) {
        return value <= max;
    }

    @Override
    public String toString() {
        return "Limits[max=" + max + "]";
    }
}

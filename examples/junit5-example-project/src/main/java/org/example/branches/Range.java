package org.example.branches;

/**
 * Stub range type for {@link BranchCaptureShapes} demonstration fixtures.
 * Checks whether a pair of integer bounds falls within this range.
 */
public final class Range {

    private final int low;
    private final int high;

    /**
     * Constructs a range with the given inclusive bounds.
     *
     * @param low  the lower inclusive bound
     * @param high the upper inclusive bound
     */
    public Range(int low, int high) {
        this.low = low;
        this.high = high;
    }

    /**
     * Returns {@code true} when both {@code min} and {@code max} are within this range.
     *
     * @param min the lower bound to check
     * @param max the upper bound to check
     * @return {@code true} if {@code low <= min} and {@code max <= high}
     */
    public boolean contains(int min, int max) {
        return low <= min && max <= high;
    }

    @Override
    public String toString() {
        return "Range[" + low + ".." + high + "]";
    }
}

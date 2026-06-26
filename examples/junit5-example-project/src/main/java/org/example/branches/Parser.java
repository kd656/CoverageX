package org.example.branches;

/**
 * Stub parser type for {@link BranchCaptureShapes} demonstration fixtures.
 * Accepts {@code double} and mixed category-1/category-2 arguments so that
 * the spill emitter's double and mixed paths can be demonstrated.
 */
public final class Parser {

    private final double threshold;

    /**
     * Constructs a parser with the given acceptance threshold.
     *
     * @param threshold the minimum score that {@link #accepts(double)} passes
     */
    public Parser(double threshold) {
        this.threshold = threshold;
    }

    /**
     * Returns {@code true} when {@code score} meets the threshold.
     *
     * @param score the double score to test
     * @return {@code true} if {@code score >= threshold}
     */
    public boolean accepts(double score) {
        return score >= threshold;
    }

    /**
     * Returns {@code true} when offset is non-negative and score meets the threshold.
     * Exercises the mixed category-1 ({@code long}) + category-2 ({@code double}) path.
     *
     * @param offset a long offset (must be &ge; 0)
     * @param score  the double score to test
     * @return {@code true} if both conditions hold
     */
    public boolean acceptsMixed(long offset, double score) {
        return offset >= 0 && score >= threshold;
    }

    @Override
    public String toString() {
        return "Parser[threshold=" + threshold + "]";
    }
}

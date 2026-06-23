package io.github.kd656.coveragex.core.instrument.stubs;

/**
 * Stub type used by method-call capture tests.
 * Accepts {@code double} and mixed-type arguments so that category-2 spilling
 * can be exercised for both single-arg and multi-arg method calls.
 */
public final class DoubleParser {

    private final double threshold;

    /**
     * Constructs a parser with the given acceptance threshold.
     *
     * @param threshold the minimum value that {@link #accepts(double)} passes
     */
    public DoubleParser(double threshold) {
        this.threshold = threshold;
    }

    /**
     * Returns {@code true} when {@code score} meets the parser's threshold.
     *
     * @param score the double score to test
     * @return {@code true} if {@code score >= threshold}
     */
    public boolean accepts(double score) {
        return score >= threshold;
    }

    /**
     * Returns {@code true} when the offset and score are both acceptable.
     *
     * @param offset a long offset value
     * @param score  a double score value
     * @return {@code true} if {@code offset >= 0 && score >= threshold}
     */
    public boolean acceptsMixed(long offset, double score) {
        return offset >= 0 && score >= threshold;
    }

    @Override
    public String toString() {
        return "P{}";
    }
}

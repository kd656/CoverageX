package io.github.kd656.coveragex.core.instrument;

/**
 * Static utility methods used as instrumentation targets for capture tests.
 *
 * <p>Each method contains exactly one conditional expression so that the emitted
 * probe count and probe layout are predictable. Tests instrument this class with
 * the {@code DefaultProbeInjector} or {@code SourceAwareProbeInjector}, invoke
 * the method under controlled conditions, and then verify that
 * {@code recordBranchHit} was called with the expected operand values.</p>
 *
 * <p>This class must not import or reference any CoverageX runtime types: it is
 * loaded by a fresh classloader in tests so that its bytecode reflects a
 * "user class" before instrumentation.</p>
 */
public final class BranchAttributionFixtures {

    private BranchAttributionFixtures() {
    }

    /**
     * Single METHOD_CALL operand: {@code s.startsWith(".")}.
     *
     * <p>The receiver {@code s} is the only capturable argument; the literal
     * {@code "."} is filtered by the analyzer. The {@code if} form forces
     * javac to emit a conditional jump ({@code IFEQ}) so that a branch probe
     * pair is allocated and the capture emitter can stash the receiver.</p>
     *
     * @param s the string to test, must not be {@code null}
     * @return {@code true} when {@code s} starts with {@code "."}
     */
    public static boolean methodCall(String s) {
        if (s.startsWith(".")) {
            return true;
        }
        return false;
    }

    /**
     * Single BINARY_COMPARE operand: {@code x > 5}.
     *
     * <p>Both {@code x} and the literal {@code 5} appear in the condition; the
     * analyzer filters the literal, leaving {@code x} as the only capturable
     * column.</p>
     *
     * @param x the integer value to compare
     * @return {@code true} when {@code x} is greater than {@code 5}
     */
    public static boolean binaryCompare(int x) {
        return x > 5;
    }

    /**
     * Single BINARY_COMPARE operand: {@code o == null}.
     *
     * <p>Compiles to {@code IFNONNULL} (fall-through when non-null, jump when
     * null). The analyzer emits {@code o} as the single capturable column.</p>
     *
     * @param o the object to test
     * @return {@code true} when {@code o} is {@code null}
     */
    public static boolean nullCheck(Object o) {
        return o == null;
    }

    /**
     * METHOD_CALL operand with two capturable values: {@code a.equals(b)}.
     *
     * <p>The receiver {@code a} and the argument {@code b} are both
     * non-literal, so the analyzer emits two column labels and the capture
     * emitter stashes both.</p>
     *
     * @param a the receiver; must not be {@code null}
     * @param b the argument to compare
     * @return {@code true} when {@code a.equals(b)}
     */
    public static boolean equalsCheck(String a, String b) {
        return a.equals(b);
    }

    /**
     * BINARY_COMPARE with two {@code double} variables: {@code score >= threshold}.
     *
     * <p>Compiles to {@code DLOAD; DLOAD; DCMPL; IFLT}. At the {@code IFLT}
     * site the original operands are gone — only the {@code -1/0/+1} CMP
     * result is on the stack. Both sides are non-literal, so the analyser
     * emits two labels and {@code binaryCaptureMask = 3}; the injector's
     * {@code default} branch must recognise the mismatch and skip capture
     * rather than record the CMP result mislabelled as {@code score}.</p>
     *
     * @param score     the score to compare
     * @param threshold the threshold to compare against
     * @return {@code true} when {@code score >= threshold}
     */
    public static boolean doubleCompareVars(double score, double threshold) {
        return score >= threshold;
    }

    /**
     * BINARY_COMPARE with two {@code long} variables: {@code a > b}.
     *
     * <p>Compiles to {@code LLOAD; LLOAD; LCMP; IFLE}. Same category-2
     * hazard as {@link #doubleCompareVars}: the {@code IFLE} sees only the
     * CMP result, so the injector must skip capture.</p>
     *
     * @param a the left-hand operand
     * @param b the right-hand operand
     * @return {@code true} when {@code a > b}
     */
    public static boolean longCompareVars(long a, long b) {
        return a > b;
    }

    /**
     * BINARY_COMPARE with two {@code float} variables: {@code x < y}.
     *
     * <p>Compiles to {@code FLOAD; FLOAD; FCMPG; IFGE}. Same category-2
     * hazard: capture must be skipped at the {@code IFGE} site.</p>
     *
     * @param x the left-hand operand
     * @param y the right-hand operand
     * @return {@code true} when {@code x < y}
     */
    public static boolean floatCompareVars(float x, float y) {
        return x < y;
    }

    /**
     * Compound condition with two operands: {@code s == null || s.isBlank()}.
     *
     * <p>The analyzer splits this into two leaf operands: a BINARY_COMPARE
     * ({@code s == null}) and a METHOD_CALL ({@code s.isBlank()}). Each
     * operand has its own probe pair and its own capture entry.</p>
     *
     * @param s the string to test
     * @return {@code true} when {@code s} is {@code null} or blank
     */
    public static boolean compound(String s) {
        return s == null || s.isBlank();
    }
}

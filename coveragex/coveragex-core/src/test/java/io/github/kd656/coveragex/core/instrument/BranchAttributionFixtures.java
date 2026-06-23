package io.github.kd656.coveragex.core.instrument;

/**
 * Static utility methods used as instrumentation targets for Phase C capture tests.
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

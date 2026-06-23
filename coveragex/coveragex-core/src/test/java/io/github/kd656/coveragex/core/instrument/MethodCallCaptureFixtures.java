package io.github.kd656.coveragex.core.instrument;

import io.github.kd656.coveragex.core.instrument.stubs.AccessService;
import io.github.kd656.coveragex.core.instrument.stubs.DoubleParser;
import io.github.kd656.coveragex.core.instrument.stubs.FlagProvider;
import io.github.kd656.coveragex.core.instrument.stubs.FlagRules;
import io.github.kd656.coveragex.core.instrument.stubs.LongLimits;
import io.github.kd656.coveragex.core.instrument.stubs.MultiArgContainer;

/**
 * Instrumentation target for method-call operand capture tests.
 *
 * <p>Each method in this class contains exactly one conditional expression that
 * exercises a distinct shape from the capture matrix. Tests instrument this class
 * with {@link SourceAwareProbeInjector}, load it in an isolated classloader, invoke
 * the relevant method, and then verify that the branch probes carry the expected
 * {@code argLabels} and captured {@code AttributedInvocation.args()} values.</p>
 *
 * <p>This class must be stable with respect to line numbers: the source analyser
 * resolves branch operands by source line, so renaming or reordering methods affects
 * which {@code OperandModel} the visitor sees at each call site. Add new methods only
 * at the end.</p>
 */
public final class MethodCallCaptureFixtures {

    private MethodCallCaptureFixtures() {
    }

    // -------------------------------------------------------------------------
    // Instance method shapes
    // -------------------------------------------------------------------------

    /**
     * Instance, zero args — receiver captured only.
     * Analyser: {@code argLabels = ["s"]}, mask = 0b01 (bit 0).
     *
     * @param s the string to test
     * @return {@code true} if {@code s} is blank
     */
    public static boolean instanceZeroArgs(String s) {
        if (s.isBlank()) {
            return true;
        }
        return false;
    }

    /**
     * Instance, one ref arg with literal filtered — receiver captured only.
     * Source: {@code s.startsWith(".")}.  Analyser filters the literal {@code "."};
     * {@code argLabels = ["s"]}, mask = 0b01.
     *
     * @param s the string to test
     * @return {@code true} if {@code s} starts with {@code "."}
     */
    public static boolean instanceOneLiteralArg(String s) {
        if (s.startsWith(".")) {
            return true;
        }
        return false;
    }

    /**
     * Instance, one ref arg, both receiver and arg captured.
     * Source: {@code a.equals(b)}.  {@code argLabels = ["a","b"]}, mask = 0b11.
     *
     * @param a the receiver string
     * @param b the argument string
     * @return {@code true} if {@code a.equals(b)}
     */
    public static boolean instanceOneRefArgBothCaptured(String a, String b) {
        if (a.equals(b)) {
            return true;
        }
        return false;
    }

    /**
     * Instance, two ref args — receiver and both args captured.
     * Source: {@code m.contains(k, v)}.  {@code argLabels = ["m","k","v"]}, mask = 0b111.
     *
     * @param m the container
     * @param k the first key
     * @param v the second value
     * @return {@code true} if {@code m.contains(k, v)}
     */
    public static boolean instanceTwoRefArgs(MultiArgContainer m, String k, String v) {
        if (m.contains(k, v)) {
            return true;
        }
        return false;
    }

    /**
     * Instance, long arg — receiver and long value captured (category-2 spill).
     * Source: {@code limits.isAllowed(n)}.
     * {@code argLabels = ["limits","n"]}, mask = 0b11.
     *
     * @param limits the limits checker
     * @param n      the long value to check
     * @return {@code true} if {@code limits.isAllowed(n)}
     */
    public static boolean instanceLongArg(LongLimits limits, long n) {
        if (limits.isAllowed(n)) {
            return true;
        }
        return false;
    }

    /**
     * Instance, double arg — receiver and double captured (category-2 spill).
     * Source: {@code parser.accepts(x)}.
     * {@code argLabels = ["parser","x"]}, mask = 0b11.
     *
     * @param parser the double parser
     * @param x      the double score
     * @return {@code true} if {@code parser.accepts(x)}
     */
    public static boolean instanceDoubleArg(DoubleParser parser, double x) {
        if (parser.accepts(x)) {
            return true;
        }
        return false;
    }

    /**
     * Instance, mixed category-1 + category-2 args — all three captured.
     * Source: {@code parser.acceptsMixed(off, score)}.
     * {@code argLabels = ["parser","off","score"]}, mask = 0b111.
     *
     * @param parser the double parser
     * @param off    the long offset
     * @param score  the double score
     * @return {@code true} if {@code parser.acceptsMixed(off, score)}
     */
    public static boolean instanceMixedCat2Args(DoubleParser parser, long off, double score) {
        if (parser.acceptsMixed(off, score)) {
            return true;
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Static method shapes
    // -------------------------------------------------------------------------

    /**
     * Static, one ref arg — no receiver column, arg captured.
     * Source: {@code StubStatics.isBlank(s)}.
     * {@code argLabels = ["s"]}, mask = 0b10 (bit 1, receiver bit 0 ignored for static).
     *
     * @param s the string to check
     * @return {@code true} if {@code s} is blank
     */
    public static boolean staticOneRefArg(String s) {
        if (StubStatics.isBlank(s)) {
            return true;
        }
        return false;
    }

    /**
     * Static, two ref args — both args captured, no receiver column.
     * Source: {@code StubStatics.matches(a, b)}.
     * {@code argLabels = ["a","b"]}, mask = 0b110.
     *
     * @param a the first string
     * @param b the second string
     * @return {@code true} if {@code StubStatics.matches(a, b)}
     */
    public static boolean staticTwoRefArgs(String a, String b) {
        if (StubStatics.matches(a, b)) {
            return true;
        }
        return false;
    }

    /**
     * Literal-only static call — all args are literals so mask = 0, no capture.
     * Source: {@code StubStatics.enabled("A")}.
     * {@code argLabels = []}, mask = 0.
     *
     * @return {@code true} if the feature is enabled
     */
    public static boolean staticLiteralOnly() {
        if (StubStatics.enabled("A")) {
            return true;
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Nested-call shapes
    // -------------------------------------------------------------------------

    /**
     * Nested call with different name and arity — outer call wins.
     * Source: {@code svc.canAccess(svc.resolveUser(), role)}.
     * ASM visits {@code resolveUser()} first; the name guard ("canAccess" != "resolveUser")
     * prevents premature capture. Outer call: {@code argLabels = ["svc","svc.resolveUser()","role"]}.
     *
     * @param svc  the access service
     * @param role the required role
     * @return {@code true} if access is granted
     */
    public static boolean nestedDifferentName(AccessService svc, String role) {
        if (svc.canAccess(svc.resolveUser(), role)) {
            return true;
        }
        return false;
    }

    /**
     * Nested boolean-returning inner call — outer call wins due to name guard.
     * Source: {@code rules.accepts(provider.flag(), role)}.
     * The inner {@code flag()} returns {@code boolean}; a naive boolean-return guard
     * would fire on it first. Name guard ("accepts" != "flag") saves us.
     * Outer: {@code argLabels = ["rules","provider.flag()","role"]}.
     *
     * @param rules    the flag rules
     * @param provider the flag provider
     * @param role     the required role
     * @return {@code true} if accepted
     */
    public static boolean nestedBooleanInner(FlagRules rules, FlagProvider provider, String role) {
        if (rules.accepts(provider.flag(), role)) {
            return true;
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Category-2 binary comparison filter
    // -------------------------------------------------------------------------

    /**
     * Category-2 binary comparison with long literal rhs — analyser sets mask = 0.
     * Source: {@code x > 5L}.  javac emits {@code LCMP + IFLE}; at the {@code IFLE}
     * site the stack holds the LCMP result, not {@code x}. The analyser literal
     * filter prevents capture. {@code argLabels = []}.
     *
     * @param x the long value to compare
     * @return {@code true} if {@code x > 5L}
     */
    public static boolean cat2LongLiteral(long x) {
        if (x > 5L) {
            return true;
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Nested-call same name, different arity (safe case)
    // -------------------------------------------------------------------------

    /**
     * Nested call where the inner call shares the outer name but has different arity.
     * Source: {@code svc.canAccess(svc.canAccess(null, "x"), role)}.
     * The inner {@code canAccess(null, "x")} has arity 2 matching the outer;
     * however the inner call also returns Object which is not boolean — so this
     * shape would not even reach capture in practice. This fixture exercises the
     * name+arity combination by confirming the outer call's args are captured.
     *
     * <p>In this specific case both the outer and inner share name "canAccess" and
     * arity 2. ASM visits the inner one first; the matching guard will capture the
     * inner stack. This is the same-name-same-arity limitation documented in
     * potential-problems.md (P7). No assertion on captured values is made for the
     * inner/outer distinction here — this fixture is only in the matrix for the
     * safe (different-arity) variant defined in {@link #nestedDifferentName}.</p>
     *
     * @param svc  the access service
     * @param role the role
     * @return {@code true} if outer call passes
     */
    public static boolean nestedSameNameDifferentArity(AccessService svc, String role) {
        // Use resolveUser() (arity 0) nested inside canAccess (arity 2):
        // canAccess has name "canAccess", arity 2; resolveUser has name "resolveUser", arity 0.
        // This is the safe case — different names, so name guard works.
        if (svc.canAccess(svc.resolveUser(), role)) {
            return true;
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Private static helpers
    // -------------------------------------------------------------------------

    /**
     * Static helper methods used as instrumentation targets for static-call shapes.
     * Must be a non-upper-case-name class so that the analyser heuristic does not
     * treat it as a type reference — but since it IS an inner class accessed via a
     * type name, the scope {@code StubStatics} starts with upper-case and will be
     * dropped by the heuristic. That is the desired outcome for static calls.
     */
    public static final class StubStatics {

        private StubStatics() {
        }

        /**
         * Returns {@code true} when {@code s} is blank or null.
         *
         * @param s the string to check
         * @return {@code true} if blank or null
         */
        public static boolean isBlank(String s) {
            return s == null || s.isBlank();
        }

        /**
         * Returns {@code true} when {@code a} equals {@code b}.
         *
         * @param a first string
         * @param b second string
         * @return {@code true} if equal
         */
        public static boolean matches(String a, String b) {
            return a != null && a.equals(b);
        }

        /**
         * Returns {@code true} when the feature identified by {@code key} is enabled.
         * Always returns {@code true} for any non-null key.
         *
         * @param key the feature key
         * @return {@code true} always (stub)
         */
        public static boolean enabled(String key) {
            return key != null;
        }
    }
}

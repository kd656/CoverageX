package org.example.branches;

/**
 * Demonstration fixtures for the CoverageX generalised method-call operand capture.
 *
 * <p>Each method exercises a distinct capture shape. When the generated coverage report
 * is opened in a browser, the popover for each branch should display the operand columns
 * and captured values described in each method's Javadoc.</p>
 *
 * <p>Notable demo targets:</p>
 * <ul>
 *   <li>{@code nestedDifferentName} — the popover shows {@code svc} / {@code resolveUser()}
 *       / {@code role} columns with the <em>outer</em> call's values.</li>
 *   <li>{@code nestedBooleanInner} — the name guard prevents the inner boolean-returning
 *       {@code flag()} call from triggering capture prematurely.</li>
 *   <li>{@code longLiteralCompare} — the analyser cat-2 filter is active; the popover
 *       shows <em>no</em> operand column for this branch.</li>
 * </ul>
 */
public final class BranchCaptureShapes {

    private BranchCaptureShapes() {
    }

    // -----------------------------------------------------------------------
    // Instance call with multiple non-literal args
    // -----------------------------------------------------------------------

    /**
     * Instance call with two non-literal args — receiver + both args captured.
     *
     * <p>Demonstrates: {@code METHOD_CALL} on {@code range.contains(min, max)} with
     * a simple receiver and two int arguments. Popover columns: {@code range},
     * {@code min}, {@code max}.</p>
     *
     * @param range the range to test
     * @param min   the lower bound to check
     * @param max   the upper bound to check
     * @return {@code true} when the range contains both bounds
     */
    public static boolean rangeContains(Range range, int min, int max) {
        if (range.contains(min, max)) {
            return true;
        }
        return false;
    }

    // -----------------------------------------------------------------------
    // Static call — no receiver column
    // -----------------------------------------------------------------------

    /**
     * Static call with one non-literal arg — arg captured; no receiver column.
     *
     * <p>Demonstrates: {@code METHOD_CALL} on {@code Strings.isBlank(name)}.
     * The type-reference scope {@code Strings} is dropped by the analyser heuristic.
     * Popover column: {@code name}.</p>
     *
     * @param name the string to test
     * @return {@code true} when {@code name} is blank
     */
    public static boolean classifyStatic(String name) {
        if (Strings.isBlank(name)) {
            return true;
        }
        return false;
    }

    /**
     * Static call with two non-literal args — both args captured.
     *
     * <p>Demonstrates: {@code METHOD_CALL} on {@code Rules.matches(a, b)}.
     * Popover columns: {@code a}, {@code b}.</p>
     *
     * @param a the first string
     * @param b the second string
     * @return {@code true} when {@code a} matches {@code b}
     */
    public static boolean rulesMatch(String a, String b) {
        if (Rules.matches(a, b)) {
            return true;
        }
        return false;
    }

    // -----------------------------------------------------------------------
    // Category-2 arguments — long and double
    // -----------------------------------------------------------------------

    /**
     * Category-2 long arg — receiver + long captured (boxed as {@code Long}).
     *
     * <p>Demonstrates: {@code METHOD_CALL} on {@code limits.isAllowed(value)} with a
     * {@code long} argument. The spill emitter uses {@code LSTORE}/{@code LLOAD} to
     * preserve the category-2 slot before the call site. Popover columns: {@code limits},
     * {@code value}.</p>
     *
     * @param limits the limits checker
     * @param value  the long value to check
     * @return {@code true} when the value is within limits
     */
    public static boolean longCheck(Limits limits, long value) {
        if (limits.isAllowed(value)) {
            return true;
        }
        return false;
    }

    /**
     * Category-2 double arg — receiver + double captured (boxed as {@code Double}).
     *
     * <p>Demonstrates: {@code METHOD_CALL} on {@code parser.accepts(score)} with a
     * {@code double} argument. The spill emitter uses {@code DSTORE}/{@code DLOAD}.
     * Popover columns: {@code parser}, {@code score}.</p>
     *
     * @param parser the score parser
     * @param score  the double score to check
     * @return {@code true} when the parser accepts the score
     */
    public static boolean doubleCheck(Parser parser, double score) {
        if (parser.accepts(score)) {
            return true;
        }
        return false;
    }

    /**
     * Mixed category-1 and category-2 — all three positions captured.
     *
     * <p>Demonstrates: {@code METHOD_CALL} on {@code parser.acceptsMixed(offset, score)}
     * with a {@code long} offset and {@code double} score. Both category-2 arguments
     * are spilled with typed {@code LSTORE}/{@code DSTORE} instructions.
     * Popover columns: {@code parser}, {@code offset}, {@code score}.</p>
     *
     * @param parser the parser
     * @param offset the long offset
     * @param score  the double score
     * @return {@code true} when both conditions are met
     */
    public static boolean mixedCheck(Parser parser, long offset, double score) {
        if (parser.acceptsMixed(offset, score)) {
            return true;
        }
        return false;
    }

    // -----------------------------------------------------------------------
    // Nested method calls — matching guard demonstration
    // -----------------------------------------------------------------------

    /**
     * Nested call with a different inner name — outer call wins.
     *
     * <p>Demonstrates: the name+arity matching guard. ASM visits
     * {@code resolveUser()} first; the guard rejects it ("canAccess" ≠ "resolveUser")
     * and capture fires on the outer {@code service.canAccess(user, role)} call.
     * Popover columns: {@code service}, {@code resolveUser()}, {@code role}.</p>
     *
     * @param service the service to query
     * @param role    the required role
     * @return {@code true} when access is granted
     */
    public static boolean nestedDifferentName(Service service, String role) {
        if (service.canAccess(resolveUser(), role)) {
            return true;
        }
        return false;
    }

    /**
     * Nested boolean-returning inner call — outer call wins via name guard.
     *
     * <p>Demonstrates: a nested call that returns {@code boolean}
     * ({@code provider.flag()}) inside the outer call's argument list.
     * A naïve boolean-return guard would match {@code flag()} first; the name guard
     * ("accepts" ≠ "flag") prevents this. Popover columns: {@code rules},
     * {@code provider.flag()}, {@code role}.</p>
     *
     * @param rules    the rules engine
     * @param provider the flag provider
     * @param role     the role to check
     * @return {@code true} when the rules accept the invocation
     */
    public static boolean nestedBooleanInner(Rules rules, FlagProvider provider, String role) {
        if (rules.accepts(provider.flag(), role)) {
            return true;
        }
        return false;
    }

    // -----------------------------------------------------------------------
    // Category-2 literal comparison — analyser filter demonstration
    // -----------------------------------------------------------------------

    /**
     * Long-vs-literal comparison — analyser filter sets {@code captureMask = 0}.
     *
     * <p>Demonstrates: the category-2 binary-compare literal filter. javac compiles
     * {@code x > 5L} as {@code LLOAD x}, {@code LDC2_W 5L}, {@code LCMP},
     * {@code IFLE}. At the {@code IFLE} site the stack holds the LCMP result, not
     * {@code x}. The analyser detects the long literal and sets
     * {@code binaryCaptureMask = 0}, preventing a mislabelled capture.
     * <em>The popover shows no operand column for this branch</em> — this is the
     * cat-2 limitation made visible in the demo report.</p>
     *
     * @param x the long value to compare against the literal {@code 5L}
     * @return {@code true} when {@code x > 5L}
     */
    public static boolean longLiteralCompare(long x) {
        if (x > 5L) {
            return true;
        }
        return false;
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Returns a stub user for the nested-call demonstration.
     *
     * @return a non-null user object
     */
    private static User resolveUser() {
        return new User(42);
    }
}

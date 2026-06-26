package org.example.branches;

import java.util.Collection;
import java.util.List;

/**
 * Demonstration fixtures for the CoverageX branch-test-attribution feature.
 *
 * <p>Each method below exercises exactly one branch-condition kind that the
 * Phase B/C {@code OperandArgVisitor} handles. When the generated coverage
 * report is opened in a browser, the popover for each branch should display
 * the operand columns and captured values described in each method's Javadoc.</p>
 *
 * <p>All methods are {@code static} and intentionally minimal (3–6 lines each)
 * so that the report stays readable as a quick reference for the feature.</p>
 */
public final class BranchFixtures {

    private BranchFixtures() {
        // static utility — do not instantiate
    }

    // -----------------------------------------------------------------------
    // METHOD_CALL operands
    // -----------------------------------------------------------------------

    /**
     * Single-arg receiver capture.
     *
     * <p>Demonstrates: {@code METHOD_CALL} with a simple receiver ({@code name})
     * and a string literal argument (dropped). Popover column: {@code name}.</p>
     *
     * @param name the string to test
     * @return {@code "dot"} when {@code name} starts with {@code "."}, otherwise {@code "plain"}
     */
    public static String checkStartsWithDot(String name) {
        if (name.startsWith(".")) {
            return "dot";
        }
        return "plain";
    }

    /**
     * Receiver + argument both captured.
     *
     * <p>Demonstrates: {@code METHOD_CALL} on {@code equals} with a non-literal
     * argument. Popover columns: {@code a}, {@code b}.</p>
     *
     * @param a first string
     * @param b second string
     * @return {@code "equal"} when {@code a.equals(b)}, otherwise {@code "different"}
     */
    public static String checkEquals(String a, String b) {
        if (a.equals(b)) {
            return "equal";
        }
        return "different";
    }

    /**
     * No-arg method call with receiver capture.
     *
     * <p>Demonstrates: {@code METHOD_CALL} on {@code isBlank()} — zero args so only
     * the receiver is captured. Popover column: {@code text}.</p>
     *
     * @param text the string to test
     * @return {@code "blank"} when {@code text.isBlank()}, otherwise {@code "non-blank"}
     */
    public static String checkIsBlank(String text) {
        if (text.isBlank()) {
            return "blank";
        }
        return "non-blank";
    }

    /**
     * Static method call as scope — scope is dropped, only the argument is captured.
     *
     * <p>Demonstrates: {@code BINARY_COMPARE} with {@code Math.abs(value)} on the
     * left and a literal on the right. {@code Math} (type reference) is dropped.
     * Popover column: {@code value}.</p>
     *
     * @param value the integer to test
     * @return {@code "big"} when {@code Math.abs(value) > 10}, otherwise {@code "small"}
     */
    public static String checkAbsGreaterThanTen(int value) {
        if (Math.abs(value) > 10) {
            return "big";
        }
        return "small";
    }

    /**
     * Method-call result used as a scope — non-simple receiver is dropped.
     *
     * <p>Demonstrates: {@code METHOD_CALL} where the receiver is itself a
     * method call ({@code getName(dotPrefix).startsWith(".")}). Both the scope and the
     * literal argument are dropped. The popover shows no operand columns.</p>
     *
     * @param dotPrefix when {@code true}, the internal name starts with {@code "."}
     * @return {@code "dot-name"} when the internal name starts with {@code "."},
     *         otherwise {@code "plain-name"}
     */
    public static String checkChainedStartsWith(boolean dotPrefix) {
        if (getName(dotPrefix).startsWith(".")) {
            return "dot-name";
        }
        return "plain-name";
    }

    // -----------------------------------------------------------------------
    // BINARY_COMPARE operands
    // -----------------------------------------------------------------------

    /**
     * LHS variable, RHS literal.
     *
     * <p>Demonstrates: {@code BINARY_COMPARE} with a variable on the left and a
     * constant on the right. Popover column: {@code x}.</p>
     *
     * @param x the integer to compare
     * @return {@code "big"} when {@code x > 5}, otherwise {@code "small"}
     */
    public static String checkGreaterThanFive(int x) {
        if (x > 5) {
            return "big";
        }
        return "small";
    }

    /**
     * LHS literal, RHS variable.
     *
     * <p>Demonstrates: {@code BINARY_COMPARE} with a constant on the left and
     * a variable on the right. The capture mask omits the literal; only
     * {@code x} (rhs) is captured. Popover column: {@code x}.</p>
     *
     * @param x the integer to compare
     * @return {@code "big"} when {@code 5 > x} (i.e. {@code x < 5}), otherwise {@code "small"}
     */
    public static String checkLiteralGreaterThanX(int x) {
        if (5 > x) {
            return "big";
        }
        return "small";
    }

    /**
     * Both sides are variables.
     *
     * <p>Demonstrates: {@code BINARY_COMPARE} where both operands are captured.
     * Popover columns: {@code a}, {@code b}.</p>
     *
     * @param a first integer
     * @param b second integer
     * @return {@code "a-wins"} when {@code a > b}, otherwise {@code "b-wins"}
     */
    public static String checkAGreaterThanB(int a, int b) {
        if (a > b) {
            return "a-wins";
        }
        return "b-wins";
    }

    /**
     * Null check — variable on the right of {@code == null}.
     *
     * <p>Demonstrates: {@code BINARY_COMPARE} null check with the variable on
     * the left. Popover column: {@code input}.</p>
     *
     * @param input the string to check
     * @return {@code "null"} when {@code input == null}, otherwise {@code "non-null"}
     */
    public static String checkNullRight(String input) {
        if (input == null) {
            return "null";
        }
        return "non-null";
    }

    /**
     * Null check — {@code null} literal on the left side.
     *
     * <p>Demonstrates: {@code BINARY_COMPARE} null check with the {@code null}
     * literal on the left. Only {@code input} (rhs) is captured.
     * Popover column: {@code input}.</p>
     *
     * @param input the string to check
     * @return {@code "null"} when {@code null == input}, otherwise {@code "non-null"}
     */
    public static String checkNullLeft(String input) {
        if (null == input) {
            return "null";
        }
        return "non-null";
    }

    /**
     * Reference equality with two variables.
     *
     * <p>Demonstrates: {@code BINARY_COMPARE} reference equality where both sides
     * are captured. Popover columns: {@code a}, {@code b}.</p>
     *
     * @param a first object
     * @param b second object
     * @return {@code "same"} when {@code a == b}, otherwise {@code "different"}
     */
    public static String checkReferenceEquality(Object a, Object b) {
        if (a == b) {
            return "same";
        }
        return "different";
    }

    // -----------------------------------------------------------------------
    // Compound conditions
    // -----------------------------------------------------------------------

    /**
     * OR short-circuit condition.
     *
     * <p>Demonstrates: two operands in an OR expression. First operand captures
     * {@code s} (lhs of {@code == null}); second operand captures {@code s}
     * (receiver of {@code isBlank()}). The popover dropdown lets the reviewer
     * switch between operands.</p>
     *
     * @param s the string to test
     * @return {@code "empty"} when {@code s == null || s.isBlank()},
     *         otherwise {@code "non-empty"}
     */
    public static String checkNullOrBlank(String s) {
        if (s == null || s.isBlank()) {
            return "empty";
        }
        return "non-empty";
    }

    /**
     * AND short-circuit condition.
     *
     * <p>Demonstrates: two operands in an AND expression. Both operands capture
     * {@code x}. Popover shows two separate operand entries, each with a
     * {@code x} column.</p>
     *
     * @param x the integer to test
     * @return {@code "in-range"} when {@code x > 0 && x < 100},
     *         otherwise {@code "out-of-range"}
     */
    public static String checkInRange(int x) {
        if (x > 0 && x < 100) {
            return "in-range";
        }
        return "out-of-range";
    }

    // -----------------------------------------------------------------------
    // Operands the visitor deliberately skips
    // -----------------------------------------------------------------------

    /**
     * Bare boolean reference — no operand columns.
     *
     * <p>Demonstrates: the visitor produces no column schema for a plain
     * {@code boolean} variable used as a condition. The popover shows bare
     * TRUE/FALSE directions without any captured arguments.</p>
     *
     * @param flag the boolean to test
     * @return {@code "on"} when {@code flag} is {@code true}, otherwise {@code "off"}
     */
    public static String checkBareBoolean(boolean flag) {
        if (flag) {
            return "on";
        }
        return "off";
    }

    /**
     * Unary negation of a method call — no operand columns.
     *
     * <p>Demonstrates: the visitor does not produce columns for a unary
     * negation ({@code !}) applied to a {@code MethodCallExpr}. The popover
     * shows bare TRUE/FALSE directions without any captured arguments.</p>
     *
     * @param list the list to test
     * @return {@code "non-empty"} when {@code !list.isEmpty()},
     *         otherwise {@code "empty"}
     */
    public static String checkNegatedIsEmpty(List<?> list) {
        if (!list.isEmpty()) {
            return "non-empty";
        }
        return "empty";
    }

    /**
     * Collection contains verification
     *
     * @param collection the list to test
     * @return {@code "contains"} when {@code collection.contains(o)},
     *         otherwise {@code "does-not-contain"}
     */
    public static String checkContainsValues(Collection<?> collection, Object o) {
        if (collection.contains(o)) {
            return "contains";
        }
        return "does-not-contain";
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Returns a synthetic name used to demonstrate a chained-scope method call.
     * Accepts a parameter so that callers can exercise both branch directions.
     *
     * @param dotPrefix when {@code true}, the returned name starts with {@code "."}
     * @return a name string
     */
    private static String getName(boolean dotPrefix) {
        return dotPrefix ? ".synthetic" : "synthetic";
    }
}

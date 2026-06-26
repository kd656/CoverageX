package org.example.branches;

/**
 * Stub rule-engine type for {@link BranchCaptureShapes} demonstration fixtures.
 * Provides both an instance predicate and a static matching method.
 */
public final class Rules {

    /**
     * Returns {@code true} when {@code a} and {@code b} match each other.
     * Static call — no receiver column in the popover.
     *
     * @param a the first string
     * @param b the second string
     * @return {@code true} if {@code a} equals {@code b}
     */
    public static boolean matches(String a, String b) {
        return a != null && a.equals(b);
    }

    /**
     * Returns {@code true} when {@code flag} is set and {@code role} is non-blank.
     *
     * @param flag a boolean flag
     * @param role the required role
     * @return {@code true} if both conditions hold
     */
    public boolean accepts(boolean flag, String role) {
        return flag && role != null && !role.isBlank();
    }

    @Override
    public String toString() {
        return "Rules{}";
    }
}

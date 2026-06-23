package org.example.branches;

/**
 * Stub string-utility type for {@link BranchCaptureShapes} demonstration fixtures.
 * Provides a static method so that the static-call shape can be demonstrated.
 */
public final class Strings {

    private Strings() {
    }

    /**
     * Returns {@code true} when {@code s} is {@code null} or blank.
     *
     * @param s the string to test
     * @return {@code true} if {@code s} is null or blank
     */
    public static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}

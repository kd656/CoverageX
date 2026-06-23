package org.example.methods;

/**
 * Fixture class for verifying per-parameter column rendering in the
 * CoverageX method-invocation popover.
 *
 * <p>Each method has named parameters that the analyze phase extracts from
 * the AST and stores in the coverage map. The invocation popover should
 * render one column per parameter ({@code NAME}, {@code THRESHOLD}, etc.)
 * rather than the generic single "Arguments" column.</p>
 */
public final class MethodParameterFixtures {

    private MethodParameterFixtures() {}

    /**
     * Classifies a string by length relative to a threshold.
     *
     * @param name      the string to classify; may be {@code null}
     * @param threshold the length boundary; strings strictly longer are "long"
     * @return {@code "null"} when {@code name} is {@code null},
     *         {@code "long"} when {@code name.length() > threshold},
     *         or {@code "short"} otherwise
     */
    public static String classify(String name, int threshold) {
        if (name == null) {
            return "null";
        }
        return name.length() > threshold ? "long" : "short";
    }

    /**
     * Formats a greeting composed of a prefix and a subject.
     *
     * @param prefix  the greeting prefix (e.g. {@code "Hello"})
     * @param subject the entity being greeted (e.g. {@code "World"})
     * @return the formatted greeting string
     */
    public static String greet(String prefix, String subject) {
        return prefix + ", " + subject + "!";
    }
}

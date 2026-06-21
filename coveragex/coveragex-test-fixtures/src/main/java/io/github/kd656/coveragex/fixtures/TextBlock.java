package io.github.kd656.coveragex.fixtures;

public final class TextBlock {

    public static String banner() {
        return """
                line one
                line two
                line three
                """;
    }

    public static void execute() {
        banner();
    }
}

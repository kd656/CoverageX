package com.coveragex.fixtures;

/**
 * JDK 22 unnamed patterns (JEP 456).
 *
 * <p>The wildcard {@code _} in a pattern binds nothing — no local-variable
 * slot is allocated for the discarded binding. javac 21 rejects {@code _}
 * in pattern position. javac 22+ emits the case block with one fewer
 * local than the equivalent {@code case Circle c -> ...} form, so the
 * analyzer's pattern handling has to treat the wildcard as a real arm
 * without an associated binding.</p>
 *
 * <p>{@code execute()} drives both sealed-hierarchy cases, so both arms
 * of the pattern switch fire at runtime.</p>
 */
public final class UnnamedPatternInSwitch {

    public sealed interface Shape permits Circle, Square {}
    public record Circle(double r) implements Shape {}
    public record Square(double s) implements Shape {}

    public static String name(Shape shape) {
        return switch (shape) {
            case Circle _ -> "circle";
            case Square _ -> "square";
        };
    }

    public static void execute() {
        name(new Circle(1.0));
        name(new Square(2.0));
    }
}

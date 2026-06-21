package io.github.kd656.coveragex.fixtures;

public final class SealedTypes {

    public sealed interface Shape permits Circle, Square {}
    public record Circle(double radius) implements Shape {}
    public record Square(double side)   implements Shape {}

    public static double area(Shape s) {
        return switch (s) {
            case Circle c -> Math.PI * c.radius() * c.radius();
            case Square sq -> sq.side() * sq.side();
        };
    }

    public static void execute() {
        area(new Circle(2.0));
        area(new Square(3.0));
    }
}

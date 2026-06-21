package io.github.kd656.coveragex.fixtures;

public final class RecordSimple {

    public record Point(int x, int y) {}

    public static void execute() {
        Point p = new Point(3, 4);
        p.x();
        p.y();
        p.hashCode();
        p.toString();
    }
}

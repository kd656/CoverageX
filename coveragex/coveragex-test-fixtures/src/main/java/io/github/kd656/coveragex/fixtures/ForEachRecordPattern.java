package io.github.kd656.coveragex.fixtures;

import java.util.List;

public final class ForEachRecordPattern {

    public record Point(int x, int y) {}

    public static int sumAll(List<Point> points) {
        int total = 0;
        for (Point p : points) {
            // Use a record pattern inside the loop body
            if (p instanceof Point(int x, int y)) {
                total += x + y;
            }
        }
        return total;
    }

    public static void execute() {
        sumAll(List.of(new Point(1, 2), new Point(3, 4)));
    }
}

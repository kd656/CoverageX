package com.coveragex.fixtures;

public final class RecordPattern {

    public record Point(int x, int y) {}

    public static int sumCoords(Object o) {
        if (o instanceof Point(int x, int y)) {
            return x + y;
        }
        return -1;
    }

    public static void execute() {
        sumCoords(new Point(3, 4));
        sumCoords("not a point");
    }
}

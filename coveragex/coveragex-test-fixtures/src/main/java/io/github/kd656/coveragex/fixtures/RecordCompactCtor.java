package io.github.kd656.coveragex.fixtures;

public final class RecordCompactCtor {

    public record Positive(int value) {
        public Positive {
            if (value <= 0) throw new IllegalArgumentException();
        }
    }

    public static void execute() {
        new Positive(5);
        try { new Positive(-1); } catch (IllegalArgumentException ignored) {}
    }
}

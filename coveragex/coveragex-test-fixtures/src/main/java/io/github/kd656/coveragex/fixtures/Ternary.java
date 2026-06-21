package io.github.kd656.coveragex.fixtures;

public final class Ternary {

    public static int abs(int v) {
        return v >= 0 ? v : -v;   // line 6 — ternary branch
    }

    public static void execute() {
        abs(3);
        abs(-3);
    }
}

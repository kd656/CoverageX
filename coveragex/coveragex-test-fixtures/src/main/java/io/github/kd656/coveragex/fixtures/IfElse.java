package io.github.kd656.coveragex.fixtures;

public final class IfElse {

    public static int classify(int v) {
        if (v > 0) {            // line 6 — branch line
            return 1;             // line 7
        }
        return -1;                // line 9
    }

    public static void execute() {
        classify(1);
        classify(-1);
    }
}

package io.github.kd656.coveragex.fixtures;

public final class VoidFn {

    private static int callCount;

    public static void doNothing() {
        callCount++;             // line 8 — sole executable line
    }

    public static void execute() {
        doNothing();
    }
}

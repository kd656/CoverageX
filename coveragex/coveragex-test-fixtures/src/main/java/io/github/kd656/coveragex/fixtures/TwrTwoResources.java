package io.github.kd656.coveragex.fixtures;

public final class TwrTwoResources {

    private static final class Res implements AutoCloseable {
        @Override public void close() {}
    }

    public static int doWork() {
        try (Res a = new Res(); Res b = new Res()) {
            return 42;
        }
    }

    public static void execute() {
        doWork();
    }
}

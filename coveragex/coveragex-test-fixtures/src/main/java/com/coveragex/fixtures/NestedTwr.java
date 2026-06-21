package com.coveragex.fixtures;

public final class NestedTwr {

    private static final class Res implements AutoCloseable {
        @Override public void close() {}
    }

    public static int doWork() {
        try (Res outer = new Res()) {
            try (Res inner = new Res()) {
                return 1;
            } catch (RuntimeException e) {
                return -1;
            } finally {
                @SuppressWarnings("unused") int marker = 0;
            }
        }
    }

    public static void execute() {
        doWork();
    }
}

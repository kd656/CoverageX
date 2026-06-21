package com.coveragex.fixtures;

public final class TryWithResources {

    public static int read(boolean shouldThrow) {
        try (var r = new AutoCloseableImpl()) {       // line 6
            if (shouldThrow) throw new RuntimeException("boom");  // line 7 — ATHROW
            return 42;                                 // line 8 — normal-exit return
        } catch (RuntimeException ignored) {
            return -1;                                 // line 10 — catch-arm return
        }
    }

    public static void execute() {
        read(false);   // normal-exit path
        read(true);    // throw + catch path
    }

    private static final class AutoCloseableImpl implements AutoCloseable {
        @Override public void close() { /* no-op */ }
    }
}

package io.github.kd656.coveragex.fixtures;

public final class TryInLambda {

    public static void execute() {
        Runnable r = () -> {
            try {
                if (Math.random() < -1) throw new RuntimeException("never");
            } catch (RuntimeException ignored) {
            }
        };
        r.run();
        Runnable thrower = () -> {
            try {
                throw new RuntimeException("boom");
            } catch (RuntimeException ignored) {
            }
        };
        thrower.run();
    }
}

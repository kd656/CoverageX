package io.github.kd656.coveragex.fixtures;

public final class Lambda {

    private static int counter;

    public static void execute() {
        Runnable r = () -> counter++;   // line 8 — invokedynamic + synthetic lambda$execute$0
        r.run();
    }
}

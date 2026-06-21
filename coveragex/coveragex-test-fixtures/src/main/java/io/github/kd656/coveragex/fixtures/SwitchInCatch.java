package io.github.kd656.coveragex.fixtures;

public final class SwitchInCatch {

    public static String handle(int kind) {
        try {
            if (kind == 1) throw new ArithmeticException();
            if (kind == 2) throw new NullPointerException();
            return "ok";
        } catch (RuntimeException e) {
            return switch (e.getClass().getSimpleName()) {
                case "ArithmeticException"     -> "math";
                case "NullPointerException"    -> "null";
                default                         -> "other";
            };
        }
    }

    public static void execute() {
        handle(0);
        handle(1);
        handle(2);
    }
}

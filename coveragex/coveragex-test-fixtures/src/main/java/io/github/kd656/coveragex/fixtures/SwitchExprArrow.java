package io.github.kd656.coveragex.fixtures;

public final class SwitchExprArrow {

    public static int label(int n) {
        return switch (n) {
            case 1 -> 10;
            case 2 -> 20;
            case 3 -> 30;
            default -> 0;
        };
    }

    public static void execute() {
        label(1);
        label(2);
        label(3);
        label(99);
    }
}

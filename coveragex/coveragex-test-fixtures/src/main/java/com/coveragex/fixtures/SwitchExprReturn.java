package com.coveragex.fixtures;

public final class SwitchExprReturn {

    public static String name(int n) {
        return switch (n) {
            case 1 -> "one";
            case 2 -> "two";
            default -> "other";
        };
    }

    public static void execute() {
        name(1);
        name(2);
        name(99);
    }
}

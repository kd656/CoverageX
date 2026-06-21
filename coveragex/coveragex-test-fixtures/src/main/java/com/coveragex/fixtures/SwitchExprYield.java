package com.coveragex.fixtures;

public final class SwitchExprYield {

    public static int classify(int n) {
        return switch (n) {
            case 1 -> 10;
            case 2 -> {
                int doubled = n * 2;
                yield doubled + 1;
            }
            default -> 0;
        };
    }

    public static void execute() {
        classify(1);
        classify(2);
        classify(9);
    }
}

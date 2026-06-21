package io.github.kd656.coveragex.fixtures;

public final class NestedIfFor {

    public static int countEvens(int n) {
        int evens = 0;
        for (int i = 0; i < n; i++) {
            if (i % 2 == 0) {
                evens++;
            }
        }
        return evens;
    }

    public static void execute() {
        countEvens(5);
    }
}

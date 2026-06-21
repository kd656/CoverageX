package com.coveragex.fixtures;

public final class StaticFns {

    public static int add(int a, int b) {
        return a + b;             // line 6
    }

    public static int square(int n) {
        return n * n;             // line 10
    }

    public static void execute() {
        add(2, 3);
        square(4);
    }
}

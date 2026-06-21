package com.coveragex.fixtures;

public final class Recursive {

    public static int factorial(int n) {
        if (n <= 1) {            // line 6 — base-case branch
            return 1;             // line 7
        }
        return n * factorial(n - 1);  // line 9
    }

    public static void execute() {
        factorial(5);             // exercises recursion 5 deep
    }
}

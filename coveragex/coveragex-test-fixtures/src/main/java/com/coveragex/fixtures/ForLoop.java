package com.coveragex.fixtures;

public final class ForLoop {

    public static int sumTo(int n) {
        int total = 0;
        for (int i = 0; i < n; i++) {     // line 7 — loop condition
            total += i;
        }
        return total;                      // line 10
    }

    public static void execute() {
        sumTo(3);   // loop body runs 3×, exits on i==3
    }
}

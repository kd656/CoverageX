package com.coveragex.fixtures;

public final class DoWhile {

    public static int countDown(int n) {
        do {
            n--;
        } while (n > 0);          // line 8 — post-test loop cond
        return n;                  // line 9
    }

    public static void execute() {
        countDown(1);             // body runs once, cond false on check
    }
}

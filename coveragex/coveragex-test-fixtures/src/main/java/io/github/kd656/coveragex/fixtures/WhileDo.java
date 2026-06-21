package io.github.kd656.coveragex.fixtures;

public final class WhileDo {

    public static int countDown(int n) {
        while (n > 0) {           // line 6 — pre-test loop cond
            n--;
        }
        return n;                  // line 9
    }

    public static void execute() {
        countDown(2);
    }
}

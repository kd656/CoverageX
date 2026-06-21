package io.github.kd656.coveragex.fixtures;

public final class ForEachArray {

    public static int sum(int[] arr) {
        int total = 0;
        for (int v : arr) {       // line 7 — synthetic counter cond
            total += v;
        }
        return total;              // line 10
    }

    public static void execute() {
        sum(new int[]{1, 2, 3});
    }
}

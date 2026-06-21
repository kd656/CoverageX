package io.github.kd656.coveragex.core.fixtures.loops;

public class ForEachArray {
    public int sumArray(int[] values) {
        int total = 0;
        for (int v : values) {
            total += v;
        }
        return total;
    }
}

package io.github.kd656.coveragex.core.fixtures.loops;

public class LoopContinue {
    public int sumPositive(int[] values) {
        int total = 0;
        for (int v : values) {
            if (v <= 0) {
                continue;
            }
            total += v;
        }
        return total;
    }
}

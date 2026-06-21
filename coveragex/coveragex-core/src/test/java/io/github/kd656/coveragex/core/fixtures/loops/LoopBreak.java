package io.github.kd656.coveragex.core.fixtures.loops;

public class LoopBreak {
    public int findFirst(int[] values, int target) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == target) {
                return i;
            }
        }
        return -1;
    }
}

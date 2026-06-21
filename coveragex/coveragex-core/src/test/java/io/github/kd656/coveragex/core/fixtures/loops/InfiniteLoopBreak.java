package io.github.kd656.coveragex.core.fixtures.loops;

public class InfiniteLoopBreak {
    public int countUntilNegative(int[] arr) {
        int i = 0;
        while (true) {
            if (i >= arr.length) break;
            if (arr[i] < 0) break;
            i++;
        }
        return i;
    }
}

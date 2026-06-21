package io.github.kd656.coveragex.core.fixtures.loops;

public class ForLoop {
    public int product(int n) {
        int result = 1;
        for (int i = 1; i <= n; i++) {
            result *= i;
        }
        return result;
    }
}

package io.github.kd656.coveragex.core.fixtures.loops;

public class WhileLoop {
    public int sum(int n) {
        int total = 0;
        int i = 0;
        while (i < n) {
            total += i;
            i++;
        }
        return total;
    }
}

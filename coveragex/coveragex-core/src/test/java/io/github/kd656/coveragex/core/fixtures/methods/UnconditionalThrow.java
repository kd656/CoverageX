package io.github.kd656.coveragex.core.fixtures.methods;

public class UnconditionalThrow {
    public void requirePositive(int x) {
        if (x <= 0) {
            throw new IllegalArgumentException("Must be positive: " + x);
        }
    }
}

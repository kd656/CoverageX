package io.github.kd656.coveragex.core.fixtures.conditions;

public class ChainedConditions {
    public boolean allPositive(int a, int b, int c, int d) {
        return a > 0 && b > 0 && c > 0 && d > 0;
    }
}

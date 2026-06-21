package io.github.kd656.coveragex.core.fixtures.methods;

public class MultipleReturns {
    public String classify(int x) {
        if (x < 0)  return "negative";
        if (x == 0) return "zero";
        if (x < 10) return "small";
        return "large";
    }
}

package io.github.kd656.coveragex.core.fixtures.conditions;

public class NestedTernary {
    public String classify(int x) {
        return x > 0 ? "positive" : x < 0 ? "negative" : "zero";
    }
}

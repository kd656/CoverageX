package io.github.kd656.coveragex.core.fixtures.conditions;

public class TernaryOp {
    public String sign(int x) {
        return x >= 0 ? "non-negative" : "negative";
    }
}

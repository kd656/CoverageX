package io.github.kd656.coveragex.core.fixtures.conditions;

public class ShortCircuitAnd {
    public String check(String s, int minLen) {
        if (s != null && s.length() >= minLen) {
            return "pass";
        }
        return "fail";
    }
}

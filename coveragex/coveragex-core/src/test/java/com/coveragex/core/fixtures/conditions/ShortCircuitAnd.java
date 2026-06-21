package com.coveragex.core.fixtures.conditions;

public class ShortCircuitAnd {
    public String check(String s, int minLen) {
        if (s != null && s.length() >= minLen) {
            return "pass";
        }
        return "fail";
    }
}

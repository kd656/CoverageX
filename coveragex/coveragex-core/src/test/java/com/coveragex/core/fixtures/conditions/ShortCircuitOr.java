package com.coveragex.core.fixtures.conditions;

public class ShortCircuitOr {
    public boolean isBlank(String s) {
        return s == null || s.isEmpty();
    }
}

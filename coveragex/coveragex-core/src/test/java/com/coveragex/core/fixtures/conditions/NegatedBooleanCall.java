package com.coveragex.core.fixtures.conditions;

public class NegatedBooleanCall {
    public String status(boolean enabled) {
        if (!enabled) {
            return "disabled";
        }
        return "enabled";
    }
}

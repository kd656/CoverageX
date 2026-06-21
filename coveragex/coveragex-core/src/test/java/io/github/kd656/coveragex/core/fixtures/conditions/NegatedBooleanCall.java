package io.github.kd656.coveragex.core.fixtures.conditions;

public class NegatedBooleanCall {
    public String status(boolean enabled) {
        if (!enabled) {
            return "disabled";
        }
        return "enabled";
    }
}

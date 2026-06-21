package io.github.kd656.coveragex.core.fixtures.conditions;

public class InstanceofCheck {
    public String describe(Object obj) {
        if (obj instanceof String s) {
            return "string: " + s;
        }
        if (obj instanceof Integer i) {
            return "int: " + i;
        }
        return "other";
    }
}

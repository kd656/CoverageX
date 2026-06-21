package io.github.kd656.coveragex.core.fixtures.conditions;

public class IfNoElse {
    public String label(int x) {
        String result = "default";
        if (x > 100) {
            result = "large";
        }
        return result;
    }
}

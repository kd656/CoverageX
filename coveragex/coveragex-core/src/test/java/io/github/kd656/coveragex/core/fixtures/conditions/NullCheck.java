package io.github.kd656.coveragex.core.fixtures.conditions;

public class NullCheck {
    public int safeLength(String s) {
        if (s == null) {
            return 0;
        }
        return s.length();
    }
}

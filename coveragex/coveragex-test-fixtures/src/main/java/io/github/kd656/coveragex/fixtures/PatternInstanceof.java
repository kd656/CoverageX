package io.github.kd656.coveragex.fixtures;

public final class PatternInstanceof {

    public static int lengthOf(Object o) {
        if (o instanceof String s) {
            return s.length();
        }
        return -1;
    }

    public static void execute() {
        lengthOf("hello");
        lengthOf(123);
    }
}

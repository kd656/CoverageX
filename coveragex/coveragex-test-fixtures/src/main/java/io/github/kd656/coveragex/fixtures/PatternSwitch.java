package io.github.kd656.coveragex.fixtures;

public final class PatternSwitch {

    public static String describe(Object o) {
        return switch (o) {
            case Integer i -> "int=" + i;
            case String s  -> "str=" + s;
            default         -> "other";
        };
    }

    public static void execute() {
        describe(42);
        describe("hi");
        describe(3.14);
    }
}

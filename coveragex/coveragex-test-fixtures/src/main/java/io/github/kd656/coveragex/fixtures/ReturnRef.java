package io.github.kd656.coveragex.fixtures;

public final class ReturnRef {

    public static String greet() {
        return "hello";           // line 6
    }

    public static void execute() {
        greet();
    }
}

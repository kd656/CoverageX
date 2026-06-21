package com.coveragex.fixtures;

public final class StringConcat {

    public static String greet(String name, int n) {
        return "hello " + name + " #" + n;   // line 6 — makeConcatWithConstants
    }

    public static void execute() {
        greet("world", 1);
    }
}

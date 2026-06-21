package com.coveragex.fixtures;

import java.util.function.Function;

public final class MethodRef {

    public static void execute() {
        Function<String, Integer> len = String::length;   // line 8 — invokedynamic
        len.apply("hello");
    }
}

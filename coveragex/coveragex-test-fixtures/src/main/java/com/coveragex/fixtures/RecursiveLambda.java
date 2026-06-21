package com.coveragex.fixtures;

import java.util.function.Function;

public final class RecursiveLambda {

    private static Function<Integer, Integer> fact;

    static {
        fact = n -> n <= 1 ? 1 : n * fact.apply(n - 1);
    }

    public static void execute() {
        fact.apply(4);
    }
}

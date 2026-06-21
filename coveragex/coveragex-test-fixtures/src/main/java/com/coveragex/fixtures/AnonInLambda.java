package com.coveragex.fixtures;

import java.util.function.Supplier;

public final class AnonInLambda {

    public static void execute() {
        Supplier<Runnable> supplier = () -> new Runnable() {
            @Override
            public void run() {
                // body
            }
        };
        supplier.get().run();
    }
}

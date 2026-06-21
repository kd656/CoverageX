package com.coveragex.fixtures;

public final class AnonClass {

    private static int counter;

    public static void execute() {
        Runnable r = new Runnable() {     // line 8 — synthetic Outer$1
            @Override
            public void run() {
                counter++;                 // line 11
            }
        };
        r.run();
    }
}

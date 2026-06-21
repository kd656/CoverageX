package com.coveragex.fixtures;

public final class StaticInit {

    static int magic;

    static {
        magic = 42;               // line 8 — <clinit>
    }

    public static int get() {
        return magic;             // line 12
    }

    public static void execute() {
        get();                    // forces class init if not already
    }
}

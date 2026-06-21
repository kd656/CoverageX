package com.coveragex.fixtures;

public final class NullCheck {

    public static String orDefault(String s) {
        if (s == null) {          // line 6 — IFNULL branch
            return "default";      // line 7
        }
        return s;                 // line 9
    }

    public static void execute() {
        orDefault(null);
        orDefault("hello");
    }
}

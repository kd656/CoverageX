package com.coveragex.fixtures;

public final class SwitchString {

    public static int order(String s) {
        switch (s) {
            case "a": return 1;          // line 7
            case "b": return 2;          // line 8
            default:  return 0;          // line 9
        }
    }

    public static void execute() {
        order("a");
        order("b");
        order("z");
    }
}

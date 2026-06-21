package com.coveragex.fixtures;

public final class SwitchInt {

    public static String label(int n) {
        switch (n) {
            case 1: return "one";       // line 7
            case 2: return "two";       // line 8
            case 3: return "three";     // line 9
            default: return "other";    // line 10
        }
    }

    public static void execute() {
        label(1);
        label(2);
        label(3);
        label(99);
    }
}

package com.coveragex.fixtures;

public final class IfElseIfElse {

    public static String sign(int v) {
        if (v > 0) {              // line 6
            return "pos";          // line 7
        } else if (v < 0) {       // line 8
            return "neg";          // line 9
        } else {
            return "zero";         // line 11
        }
    }

    public static void execute() {
        sign(1);
        sign(-1);
        sign(0);
    }
}

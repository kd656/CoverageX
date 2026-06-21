package com.coveragex.core.fixtures.methods;

public class StaticMethod {
    public static int max(int a, int b) {
        return a >= b ? a : b;
    }

    public static String nullSafe(String s) {
        return s != null ? s : "";
    }
}

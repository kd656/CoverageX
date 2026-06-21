package io.github.kd656.coveragex.fixtures;

public final class TryCatchFinally {

    private static int finallyCount;

    public static int parse(String s) {
        try {
            return Integer.parseInt(s);   // line 9
        } catch (NumberFormatException ignored) {
            return -1;                    // line 11
        } finally {
            finallyCount++;               // line 13 — runs on both paths
        }
    }

    public static void execute() {
        parse("42");
        parse("nope");
    }
}

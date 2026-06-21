package io.github.kd656.coveragex.fixtures;

public final class TryCatch {

    public static int parse(String s) {
        try {
            return Integer.parseInt(s);   // line 7
        } catch (NumberFormatException ignored) {
            return -1;                    // line 9
        }
    }

    public static void execute() {
        parse("42");      // normal path
        parse("nope");    // throw + catch path
    }
}

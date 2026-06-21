package io.github.kd656.coveragex.fixtures;

public final class StaticInitTryCatch {

    static int result;

    static {
        try {
            result = Integer.parseInt("42");
        } catch (NumberFormatException e) {
            result = -1;
        }
    }

    public static int get() {
        return result;
    }

    public static void execute() {
        get();
    }
}

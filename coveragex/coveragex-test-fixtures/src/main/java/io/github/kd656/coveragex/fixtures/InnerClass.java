package io.github.kd656.coveragex.fixtures;

public final class InnerClass {

    public static final class Inner {
        public int doubled(int v) {
            return v * 2;             // line 7
        }
    }

    public static void execute() {
        new Inner().doubled(21);
    }
}

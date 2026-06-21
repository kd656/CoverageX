package io.github.kd656.coveragex.fixtures;

public final class AssertStmt {

    public static int positive(int v) {
        assert v > 0 : "must be positive";   // line 6 — $assertionsDisabled branch
        return v;                             // line 7
    }

    public static void execute() {
        // Note: assertions only fire when the JVM is started with -ea.
        // Surefire enables assertions in tests by default.
        positive(5);
    }
}

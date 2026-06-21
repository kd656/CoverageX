package com.coveragex.fixtures;

/**
 * JDK 25 flexible constructor bodies (JEP 513).
 *
 * <p>Statements before {@code this(...)} are illegal under JDK 21's javac
 * — this fixture exists only to verify the analyzer / recorder still produce
 * the right contracts when javac 25 lowers the pre-this() statements into
 * the ctor's bytecode.</p>
 *
 * <p>Source ({@code FlexibleCtorBody.java}):
 * <pre>
 *   8  public FlexibleCtorBody(int raw) {
 *   9      int normalized = raw &lt; 0 ? 0 : raw;   // ← pre-this() statement
 *  10      this(normalized, true);                   // ← chained ctor call
 *  11  }
 * </pre>
 */
public final class FlexibleCtorBody {

    private final int value;

    public FlexibleCtorBody(int raw) {
        int normalized = raw < 0 ? 0 : raw;   // line 24 — pre-this() statement + ternary branch
        this(normalized, true);                // line 25
    }

    public FlexibleCtorBody(int v, boolean tag) {
        this.value = v;
    }

    public int value() {
        return value;
    }

    public static void execute() {
        new FlexibleCtorBody(-5);   // exercises the normalize-to-0 ternary TRUE arm
        new FlexibleCtorBody(10);   // exercises the keep-as-is ternary FALSE arm
    }
}

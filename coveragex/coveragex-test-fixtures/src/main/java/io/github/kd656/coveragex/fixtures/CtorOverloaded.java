package io.github.kd656.coveragex.fixtures;

public final class CtorOverloaded {

    private final int value;

    public CtorOverloaded() {
        this(0);                  // line 8
    }

    public CtorOverloaded(int value) {
        this.value = value;       // line 12
    }

    public int value() { return value; }

    public static void execute() {
        new CtorOverloaded();
        new CtorOverloaded(7);
    }
}

package io.github.kd656.coveragex.fixtures;

public final class InstanceInit {

    private final int value;

    {
        // Instance initializer — inlined into every <init>.
        // (No reference to constructor params here so the block can stand on its own.)
        System.identityHashCode(this);       // line 10
    }

    public InstanceInit()      { this.value = 0; }   // line 13
    public InstanceInit(int v) { this.value = v; }   // line 14

    public int value() { return value; }

    public static void execute() {
        new InstanceInit();
        new InstanceInit(7);
    }
}

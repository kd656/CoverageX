package io.github.kd656.coveragex.fixtures;

public final class CtorDefault {

    // No explicit constructor — javac synthesises a public no-arg <init>.

    public static void execute() {
        new CtorDefault();        // line 8
    }
}

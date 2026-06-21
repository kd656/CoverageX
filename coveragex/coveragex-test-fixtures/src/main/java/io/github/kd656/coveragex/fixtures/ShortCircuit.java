package io.github.kd656.coveragex.fixtures;

public final class ShortCircuit {

    public static boolean both(int a, int b) {
        return a > 0 && b > 0;    // line 6 — short-circuit AND
    }

    public static boolean either(int a, int b) {
        return a > 0 || b > 0;    // line 10 — short-circuit OR
    }

    public static void execute() {
        both(1, 1);   // (T,T) → true
        both(1, 0);   // (T,F) → false (second operand evaluated)
        both(0, 0);   // (F,_) → false (short-circuit)
        either(0, 1); // (F,T) → true (second operand evaluated)
        either(1, 0); // (T,_) → true (short-circuit)
    }
}

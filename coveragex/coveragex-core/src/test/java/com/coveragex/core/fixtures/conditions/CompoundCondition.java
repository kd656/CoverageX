package com.coveragex.core.fixtures.conditions;

public class CompoundCondition {
    public String evaluate(boolean a, boolean b, boolean c) {
        if (a && b || c) {
            return "yes";
        }
        return "no";
    }
}

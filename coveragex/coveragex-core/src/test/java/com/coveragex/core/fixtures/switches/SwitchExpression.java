package com.coveragex.core.fixtures.switches;

public class SwitchExpression {
    public String describe(int code) {
        return switch (code) {
            case 0  -> "zero";
            case 1  -> "one";
            default -> "many";
        };
    }
}

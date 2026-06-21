package com.coveragex.core.fixtures.conditions;

public class NestedIf {
    public String grade(int score) {
        if (score >= 90) {
            return "A";
        } else if (score >= 75) {
            return "B";
        } else if (score >= 60) {
            return "C";
        } else {
            return "F";
        }
    }
}

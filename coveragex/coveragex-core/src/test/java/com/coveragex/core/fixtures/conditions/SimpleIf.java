package com.coveragex.core.fixtures.conditions;

public class SimpleIf {
    public String classify(int x) {
        if (x > 0) {
            return "positive";
        } else {
            return "non-positive";
        }
    }
}

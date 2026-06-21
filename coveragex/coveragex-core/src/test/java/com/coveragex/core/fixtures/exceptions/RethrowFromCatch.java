package com.coveragex.core.fixtures.exceptions;

public class RethrowFromCatch {
    public int parse(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("invalid: " + s, e);
        }
    }
}

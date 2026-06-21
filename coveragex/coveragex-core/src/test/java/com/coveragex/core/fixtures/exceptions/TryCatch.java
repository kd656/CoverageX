package com.coveragex.core.fixtures.exceptions;

public class TryCatch {
    public int parse(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}

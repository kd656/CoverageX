package com.coveragex.core.fixtures.exceptions;

public class FinallyExecutes {
    public String divide(int a, int b) {
        String result = "";
        try {
            result = String.valueOf(a / b);
        } catch (ArithmeticException e) {
            result = "error";
        } finally {
            result = result + "!";
        }
        return result;
    }
}

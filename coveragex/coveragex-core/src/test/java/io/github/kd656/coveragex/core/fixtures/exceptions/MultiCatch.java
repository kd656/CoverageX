package io.github.kd656.coveragex.core.fixtures.exceptions;

public class MultiCatch {
    public String convert(String s) {
        try {
            int val = Integer.parseInt(s);
            char ch = s.charAt(0);
            return val + ":" + ch;
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            return "error";
        }
    }
}

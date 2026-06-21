package io.github.kd656.coveragex.core.fixtures.switches;

public class SwitchString {
    public int priority(String level) {
        switch (level) {
            case "HIGH":   return 1;
            case "MEDIUM": return 2;
            case "LOW":    return 3;
            default:       return 0;
        }
    }
}

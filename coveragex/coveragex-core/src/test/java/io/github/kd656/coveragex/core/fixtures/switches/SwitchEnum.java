package io.github.kd656.coveragex.core.fixtures.switches;

public class SwitchEnum {
    public enum Priority { LOW, MEDIUM, HIGH }

    public int score(Priority p) {
        switch (p) {
            case LOW:    return 1;
            case MEDIUM: return 5;
            case HIGH:   return 10;
            default:     return 0;
        }
    }
}

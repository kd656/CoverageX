package io.github.kd656.coveragex.core.fixtures.switches;

public class SwitchLookup {
    public String classify(int code) {
        switch (code) {
            case 1:    return "one";
            case 100:  return "hundred";
            case 1000: return "thousand";
            default:   return "other";
        }
    }
}

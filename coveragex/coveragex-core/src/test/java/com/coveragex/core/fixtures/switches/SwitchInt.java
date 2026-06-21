package com.coveragex.core.fixtures.switches;

public class SwitchInt {
    public String dayName(int day) {
        switch (day) {
            case 1: return "Monday";
            case 2: return "Tuesday";
            case 3: return "Wednesday";
            default: return "Other";
        }
    }
}

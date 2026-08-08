package org.example;

public class NestedClassFixtures {

    public String normalize(String value) {
        return value == null || value.isBlank() ? "unknown" : value.trim();
    }

    public static class Multiplier {

        public int doubleIfPositive(int value) {
            return value > 0 ? value * 2 : value;
        }
    }
}

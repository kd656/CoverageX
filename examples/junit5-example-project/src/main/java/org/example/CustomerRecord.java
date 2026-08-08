package org.example;

public record CustomerRecord(String name, int age) {

    public boolean adult() {
        return age >= 18;
    }
}

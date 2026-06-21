package com.coveragex.core.fixtures.methods;

public class Constructor {
    private final String name;
    private final int value;

    public Constructor() {
        this("default", 0);
    }

    public Constructor(String name, int value) {
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        this.name = name;
        this.value = value;
    }

    public String getName() { return name; }
    public int getValue()   { return value; }
}

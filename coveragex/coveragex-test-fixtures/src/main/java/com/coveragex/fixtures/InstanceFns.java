package com.coveragex.fixtures;

public final class InstanceFns {

    private final String prefix;

    public InstanceFns(String prefix) {
        this.prefix = prefix;     // line 8
    }

    public String greet(String name) {
        return prefix + " " + name;   // line 12
    }

    public static void execute() {
        InstanceFns x = new InstanceFns("hello");
        x.greet("world");
    }
}

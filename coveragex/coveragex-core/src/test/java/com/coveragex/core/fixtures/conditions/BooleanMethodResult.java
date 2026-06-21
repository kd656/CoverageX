package com.coveragex.core.fixtures.conditions;

import java.util.List;

public class BooleanMethodResult {
    private final List<String> items;

    public BooleanMethodResult() {
        this(List.of());
    }

    public BooleanMethodResult(List<String> items) {
        this.items = items;
    }

    public static BooleanMethodResult withEmpty() { return new BooleanMethodResult(List.of()); }
    public static BooleanMethodResult withItems() { return new BooleanMethodResult(List.of("a")); }

    public String describe() {
        if (items.isEmpty()) {
            return "empty";
        }
        return "has items";
    }
}

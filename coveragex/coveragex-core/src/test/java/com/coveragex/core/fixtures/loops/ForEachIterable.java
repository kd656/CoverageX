package com.coveragex.core.fixtures.loops;

import java.util.List;

public class ForEachIterable {
    public String join(List<String> items) {
        StringBuilder sb = new StringBuilder();
        for (String item : items) {
            sb.append(item);
        }
        return sb.toString();
    }
}

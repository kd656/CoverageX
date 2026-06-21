package io.github.kd656.coveragex.fixtures;

import java.util.List;

public final class VarForEach {

    public static int total(List<String> items) {
        int n = 0;
        for (var s : items) {
            n += s.length();
        }
        return n;
    }

    public static void execute() {
        total(List.of("a", "bb", "ccc"));
    }
}

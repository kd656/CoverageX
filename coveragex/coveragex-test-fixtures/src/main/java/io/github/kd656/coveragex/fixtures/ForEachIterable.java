package io.github.kd656.coveragex.fixtures;

import java.util.List;

public final class ForEachIterable {

    public static int len(Iterable<String> items) {
        int n = 0;
        for (String s : items) {  // line 9 — Iterator.hasNext() branch
            n += s.length();
        }
        return n;                  // line 12
    }

    public static void execute() {
        len(List.of("a", "bb"));
    }
}

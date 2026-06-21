package com.coveragex.fixtures;

import java.util.List;

public final class VarLocal {

    public static int sum() {
        var list = List.of(1, 2, 3);
        var total = 0;
        for (var v : list) total += v;
        return total;
    }

    public static void execute() {
        sum();
    }
}

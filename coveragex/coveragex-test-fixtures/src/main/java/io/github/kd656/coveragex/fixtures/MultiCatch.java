package io.github.kd656.coveragex.fixtures;

import java.io.IOException;

public final class MultiCatch {

    public static int classify(int kind) throws IOException {
        try {
            if (kind == 1) throw new IOException("io");           // line 9
            if (kind == 2) throw new IllegalArgumentException();  // line 10
            return 0;                                              // line 11
        } catch (IOException | IllegalArgumentException ignored) { // line 12 — multi-catch
            return -1;                                             // line 13
        }
    }

    public static void execute() throws IOException {
        classify(0);   // no throw
        classify(1);   // IOException → caught
        classify(2);   // IAE → caught
    }
}

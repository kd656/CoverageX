package com.coveragex.fixtures;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;

public final class LambdaCaptureLoop {

    public static int sumOfCaptured() {
        List<IntSupplier> suppliers = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            int captured = i;
            suppliers.add(() -> captured);
        }
        int total = 0;
        for (IntSupplier s : suppliers) total += s.getAsInt();
        return total;
    }

    public static void execute() {
        sumOfCaptured();
    }
}

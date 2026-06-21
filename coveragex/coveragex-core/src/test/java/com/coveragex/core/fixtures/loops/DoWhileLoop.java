package com.coveragex.core.fixtures.loops;

public class DoWhileLoop {
    public int countDown(int start) {
        int count = 0;
        do {
            count++;
            start--;
        } while (start > 0);
        return count;
    }
}

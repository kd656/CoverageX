package com.coveragex.core.fixtures.exceptions;

public class TryCatchFinally {
    public String attempt(boolean shouldThrow) {
        try {
            if (shouldThrow) {
                throw new RuntimeException("forced");
            }
            return "ok";
        } catch (RuntimeException e) {
            return "caught";
        } finally {
            // compiler duplicates finally block in bytecode
        }
    }
}

package com.coveragex.test.api;

import com.coveragex.api.context.ProbeExecutionContext;
import java.util.Optional;

public final class TestContextHolder {

    private TestContextHolder() {}

    private static final InheritableThreadLocal<ProbeExecutionContext> CURRENT =
            new InheritableThreadLocal<>();

    public static void set(ProbeExecutionContext ctx) {
        CURRENT.set(ctx);
    }

    public static Optional<ProbeExecutionContext> get() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static void clear() {
        CURRENT.remove();
    }
}

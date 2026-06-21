package io.github.kd656.coveragex.test.api;

import io.github.kd656.coveragex.api.context.ProbeExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Optional;
import java.util.concurrent.Callable;

public final class TestContextPropagator {

    private static final Logger LOG = LoggerFactory.getLogger(TestContextPropagator.class);

    private TestContextPropagator() {}

    public static Runnable wrap(Runnable runnable) {
        Optional<ProbeExecutionContext> captured = TestContextHolder.get();
        if (captured.isEmpty()) {
            return runnable;
        }

        ProbeExecutionContext ctx = captured.get();
        return () -> {
            ProbeExecutionContext previous = TestContextHolder.get().orElse(null);
            TestContextHolder.set(ctx);
            try {
                runnable.run();
            } catch (Exception e) {
                LOG.warn("Task failed under probe context [{}]", ctx.id(), e);
                throw e;
            } finally {
                if (previous == null) {
                    TestContextHolder.clear();
                } else {
                    TestContextHolder.set(previous);
                }
            }
        };
    }

    public static <T> Callable<T> wrap(Callable<T> callable) {
        Optional<ProbeExecutionContext> captured = TestContextHolder.get();
        if (captured.isEmpty()) {
            return callable;
        }

        ProbeExecutionContext ctx = captured.get();

        return () -> {
            ProbeExecutionContext previous = TestContextHolder.get().orElse(null);
            TestContextHolder.set(ctx);
            try {
                return callable.call();
            } catch (Exception e) {
                LOG.warn("Task failed under probe context [{}]", ctx.id(), e);
                throw e;
            } finally {
                if (previous == null) {
                    TestContextHolder.clear();
                } else {
                    TestContextHolder.set(previous);
                }
            }
        };
    }
}

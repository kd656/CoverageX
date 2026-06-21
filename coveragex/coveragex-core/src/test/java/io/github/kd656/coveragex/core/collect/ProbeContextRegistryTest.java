package io.github.kd656.coveragex.core.collect;

import io.github.kd656.coveragex.api.context.ContextKey;
import io.github.kd656.coveragex.api.context.ProbeExecutionContext;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class ProbeContextRegistryTest {

    @Test
    void currentIsEmptyWhenNoProviderInstalled() {
        var registry = new ProbeContextRegistry();
        assertThat(registry.current()).isEmpty();
    }

    @Test
    void installGlobalSetsTheDefaultProvider() {
        var registry = new ProbeContextRegistry();
        var ctx = stubContext("test-A");

        registry.installGlobal(() -> Optional.of(ctx));

        assertThat(registry.current()).contains(ctx);
    }

    @Test
    void scopedProviderTakesPrecedenceOverGlobalOnSameThread() {
        var registry = new ProbeContextRegistry();
        var global = stubContext("global");
        var scoped = stubContext("scoped");
        registry.installGlobal(() -> Optional.of(global));

        try (var scope = registry.scope(() -> Optional.of(scoped))) {
            assertThat(registry.current()).contains(scoped);
        }
        assertThat(registry.current()).contains(global);
    }

    @Test
    void scopeOnOneThreadDoesNotAffectOtherThreads() throws Exception {
        var registry = new ProbeContextRegistry();
        var globalCtx = stubContext("global");
        var workerCtx = stubContext("worker");
        registry.installGlobal(() -> Optional.of(globalCtx));

        var workerInsideScope = new CountDownLatch(1);
        var mainObserved = new CountDownLatch(1);
        var mainSawCtx = new AtomicReference<ProbeExecutionContext>();
        var workerSawCtx = new AtomicReference<ProbeExecutionContext>();

        Thread worker = new Thread(() -> {
            try (var scope = registry.scope(() -> Optional.of(workerCtx))) {
                workerInsideScope.countDown();
                mainObserved.await();
                workerSawCtx.set(registry.current().orElse(null));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "context-registry-test-worker");
        worker.start();

        workerInsideScope.await();
        mainSawCtx.set(registry.current().orElse(null));
        mainObserved.countDown();
        worker.join();

        assertThat(mainSawCtx.get()).isSameAs(globalCtx);
        assertThat(workerSawCtx.get()).isSameAs(workerCtx);
    }

    private static ProbeExecutionContext stubContext(String id) {
        return new ProbeExecutionContext() {
            @Override public String id() { return id; }
            @Override public <T> Optional<T> get(ContextKey<T> key) { return Optional.empty(); }
            @Override public Set<ContextKey<?>> keys() { return Set.of(); }
        };
    }
}

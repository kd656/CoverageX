package com.coveragex.core.collect;

import com.coveragex.api.context.ProbeExecutionContext;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Resolves the "which test is currently running" context for the calling thread.
 *
 * <p>Mirrors {@link ProbeRecorderRegistry} in shape: a global slot plus a
 * thread-local override. The recorder reads {@link #current()} on every hit
 * to attribute it to the active test context; production installs a
 * single global provider at premain, tests open a per-thread scope.</p>
 *
 * <p>Splitting context from the recorder (rather than baking it into each
 * recorder's constructor) keeps the two lifecycles independent — a test can
 * switch context mid-execution by opening another scope without rebuilding
 * its recorder, and the recorder doesn't need to know about context plumbing.</p>
 */
public final class ProbeContextRegistry {

    private static final Supplier<Optional<ProbeExecutionContext>> NO_CONTEXT = Optional::empty;

    private final AtomicReference<Supplier<Optional<ProbeExecutionContext>>> globalProvider =
            new AtomicReference<>(NO_CONTEXT);

    /**
     * {@link InheritableThreadLocal} for the same reason
     * {@link ProbeRecorderRegistry} uses one: child threads (virtual or
     * platform) need to see the parent's context so attribution covers code
     * the test spawned, not just code on the test thread itself.
     */
    private final ThreadLocal<Supplier<Optional<ProbeExecutionContext>>> threadLocalProvider =
            new InheritableThreadLocal<>();

    /**
     * Returns the context active on the calling thread, or empty when none is set.
     *
     * <p>Thread-local takes precedence over global, matching {@link ProbeRecorderRegistry#current()}.</p>
     */
    public Optional<ProbeExecutionContext> current() {
        Supplier<Optional<ProbeExecutionContext>> local = threadLocalProvider.get();
        return (local != null ? local : globalProvider.get()).get();
    }

    /**
     * Installs the context provider used by every thread that has not opened a scope.
     */
    public void installGlobal(Supplier<Optional<ProbeExecutionContext>> provider) {
        globalProvider.set(Objects.requireNonNull(provider, "provider must not be null"));
    }

    /**
     * Installs a context provider for the calling thread only.
     *
     * <p>Use with try-with-resources. Nested scopes restore the previous
     * thread-local provider on close (not the global).</p>
     */
    public ContextScope scope(Supplier<Optional<ProbeExecutionContext>> provider) {
        Objects.requireNonNull(provider, "provider must not be null");
        Supplier<Optional<ProbeExecutionContext>> previous = threadLocalProvider.get();
        threadLocalProvider.set(provider);
        return new ContextScope(this, previous);
    }

    public static final class ContextScope implements AutoCloseable {

        private final ProbeContextRegistry registry;
        private final Supplier<Optional<ProbeExecutionContext>> previous;
        private boolean closed;

        ContextScope(ProbeContextRegistry registry, Supplier<Optional<ProbeExecutionContext>> previous) {
            this.registry = registry;
            this.previous = previous;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            if (previous == null) {
                registry.threadLocalProvider.remove();
            } else {
                registry.threadLocalProvider.set(previous);
            }
        }
    }
}

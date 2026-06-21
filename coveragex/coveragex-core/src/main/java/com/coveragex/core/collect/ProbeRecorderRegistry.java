package com.coveragex.core.collect;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Routes {@link ProbeRecorder#recordHit recordHit} calls to whichever recorder
 * is active for the calling thread.
 *
 * <p>Two slots, with thread-local winning over the global:</p>
 * <ul>
 *   <li><b>Global</b> — what the agent's premain installs at startup. Shared by
 *       every thread that has not opened a {@link RecorderScope}.</li>
 *   <li><b>Thread-local</b> — installed for a single thread only by opening a
 *       {@link #scope(ProbeRecorder) scope}. Used by tests that need an isolated
 *       recorder per worker without touching the global slot or other threads.</li>
 * </ul>
 *
 * <p>This separation is the seam that enables parallel test execution
 * without the compat-test matrix scribbling over a shared singleton.
 * See {@code documentation/coveragex-runtime-capture-contracts-plan.md} §3
 * for the design rationale.</p>
 */
public final class ProbeRecorderRegistry {

    private final AtomicReference<ProbeRecorder> globalRecorder =
            new AtomicReference<>(NoOpProbeRecorder.INSTANCE);

    /**
     * {@link InheritableThreadLocal} rather than {@link ThreadLocal} so that
     * child threads — including virtual threads — see the parent's scoped
     * recorder. Without inheritance, instrumented code running on a thread
     * the test spawned (e.g. {@code Thread.startVirtualThread(...)}) would
     * fall through to the global slot and miss the test's recorder entirely.
     */
    private final ThreadLocal<ProbeRecorder> threadLocalRecorder = new InheritableThreadLocal<>();

    /**
     * Resolves the active recorder for the calling thread.
     *
     * <p>Thread-local takes precedence over global. Production threads (which
     * never open a scope) always fall through to the global slot.</p>
     */
    public ProbeRecorder current() {
        ProbeRecorder local = threadLocalRecorder.get();
        return local != null ? local : globalRecorder.get();
    }

    /**
     * Returns the recorder currently installed in the global slot.
     *
     * <p>Used by lifecycle paths (flush, smoke tests) that need to reach the
     * production collector regardless of any thread-local scope active on the
     * calling thread.</p>
     */
    public ProbeRecorder globalRecorder() {
        return globalRecorder.get();
    }

    /**
     * Installs the recorder used by every thread that has not opened a scope.
     *
     * <p>Called once from {@code CoverageAgent.premain}. Calling it more than
     * once replaces the previous global; existing thread-local scopes are
     * unaffected.</p>
     */
    public void installGlobal(ProbeRecorder recorder) {
        globalRecorder.set(Objects.requireNonNull(recorder, "recorder must not be null"));
    }

    /**
     * Installs a recorder for the calling thread only.
     *
     * <p>Use with try-with-resources so the override is cleared automatically
     * when the scope ends, including on exception. Nested scopes restore the
     * previous thread-local value when they close (not the global), so
     * scopes compose correctly.</p>
     */
    public RecorderScope scope(ProbeRecorder recorder) {
        Objects.requireNonNull(recorder, "recorder must not be null");
        ProbeRecorder previous = threadLocalRecorder.get();
        threadLocalRecorder.set(recorder);
        return new RecorderScope(this, previous);
    }

    /**
     * Handle returned by {@link #scope(ProbeRecorder)}.
     *
     * <p>Closing the scope restores the calling thread's previous thread-local
     * slot — either the prior nested scope's recorder, or {@code null} (which
     * means "fall through to global on the next {@link #current()} call").</p>
     */
    public static final class RecorderScope implements AutoCloseable {

        private final ProbeRecorderRegistry registry;
        private final ProbeRecorder previous;
        private boolean closed;

        RecorderScope(ProbeRecorderRegistry registry, ProbeRecorder previous) {
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
                registry.threadLocalRecorder.remove();
            } else {
                registry.threadLocalRecorder.set(previous);
            }
        }
    }
}

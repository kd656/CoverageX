package com.coveragex.core.collect;

/**
 * Bytecode entry point for runtime coverage recording.
 *
 * <p>The FQN {@code com/coveragex/core/collect/CoverageDataCollectorDelegate}
 * and the {@link #recordHit recordHit} signature are baked into every
 * instrumented class file the agent has ever produced — they cannot change
 * without breaking previously-instrumented bytecode. Every other concern
 * (recorder choice, test-context resolution, lifecycle) lives behind the
 * registries this class exposes, so production and tests can compose those
 * concerns per-thread without touching the static entry point.</p>
 *
 * <p>See {@code documentation/coveragex-runtime-capture-contracts-plan.md} §3
 * for the design.</p>
 */
public final class CoverageDataCollectorDelegate {

    public static final String COLLECTOR_OWNER_CLASS =
            "com/coveragex/core/collect/CoverageDataCollectorDelegate";

    public static final String RECORD_HIT_DESCRIPTOR =
            "(Ljava/lang/String;Ljava/lang/String;I[Ljava/lang/Object;)V";

    private static final ProbeRecorderRegistry REGISTRY = new ProbeRecorderRegistry();
    private static final ProbeContextRegistry CONTEXT_REGISTRY = new ProbeContextRegistry();

    private CoverageDataCollectorDelegate() {}

    /**
     * Bytecode entry point. Routes the hit to whichever recorder is active for
     * the calling thread (thread-local override, then global).
     */
    public static void recordHit(String classId, String methodName, int probeId, Object[] args) {
        REGISTRY.current().recordHit(classId, methodName, probeId, args);
    }

    /**
     * Returns the registry that routes {@link #recordHit} calls to the active
     * recorder for the calling thread.
     */
    public static ProbeRecorderRegistry registry() {
        return REGISTRY;
    }

    /**
     * Returns the registry that resolves the active test-execution context
     * for the calling thread.
     */
    public static ProbeContextRegistry contextRegistry() {
        return CONTEXT_REGISTRY;
    }
}

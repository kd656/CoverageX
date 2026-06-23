package io.github.kd656.coveragex.core.collect;

/**
 * Bytecode-facing entry points for probe recording. One static method per
 * {@link io.github.kd656.coveragex.api.data.ProbeMetadata} kind; the
 * corresponding {@link io.github.kd656.coveragex.core.instrument.ProbeInjectionSupport}
 * {@code inject*} emitter chooses the right one at instrumentation time.
 *
 * <p>Splitting the surface area this way removes the previous single
 * {@code recordHit} entry point and lets each kind's signature carry only the
 * parameters it needs. New probe kinds add a new method instead of a new enum
 * constant plus a new dispatch arm.</p>
 *
 * <p>Every other concern (recorder choice, test-context resolution, lifecycle)
 * lives behind the registries this class exposes, so production and tests can
 * compose those concerns per-thread without touching the static entry points.</p>
 */
public final class CoverageDataCollectorDelegate {

    /** Internal class name baked into every instrumented class file. */
    public static final String COLLECTOR_OWNER_CLASS =
            "io/github/kd656/coveragex/core/collect/CoverageDataCollectorDelegate";

    /**
     * Descriptor for {@link #recordMethodEntry(String, String, int, Object[])}.
     * Baked into instrumented class files at the method-entry probe call site.
     */
    public static final String METHOD_ENTRY_DESCRIPTOR =
            "(Ljava/lang/String;Ljava/lang/String;I[Ljava/lang/Object;)V";

    /**
     * Descriptor for {@link #recordBranchHit(String, int, Object[])}.
     * Baked into instrumented class files at branch probe call sites.
     */
    public static final String BRANCH_HIT_DESCRIPTOR =
            "(Ljava/lang/String;I[Ljava/lang/Object;)V";

    /**
     * Descriptor for {@link #recordSimpleHit(String, int)}.
     * Baked into instrumented class files at return, throw, and segment probe
     * call sites.
     */
    public static final String SIMPLE_HIT_DESCRIPTOR =
            "(Ljava/lang/String;I)V";

    private static final ProbeRecorderRegistry REGISTRY = new ProbeRecorderRegistry();
    private static final ProbeContextRegistry CONTEXT_REGISTRY = new ProbeContextRegistry();

    private CoverageDataCollectorDelegate() {
    }

    /**
     * Records a method-entry probe hit together with the captured argument
     * values. Method-entry hits are the only ones that contribute to the
     * per-method invocation report.
     *
     * @param classId    the internal class name
     * @param methodName the simple method name
     * @param probeId    the probe index within the class
     * @param args       the boxed argument values, never {@code null}
     */
    public static void recordMethodEntry(String classId,
                                         String methodName,
                                         int probeId,
                                         Object[] args) {
        REGISTRY.current().recordMethodEntry(classId, methodName, probeId, args);
    }

    /**
     * Records a branch-direction probe hit together with any operand values
     * that the capture emitter stashed (may be an empty array when no source
     * map was available at instrumentation time).
     *
     * @param classId       the internal class name
     * @param probeId       the probe index within the class
     * @param operandValues the captured operand values, never {@code null};
     *                      empty when no operand capture was performed
     */
    public static void recordBranchHit(String classId,
                                        int probeId,
                                        Object[] operandValues) {
        REGISTRY.current().recordBranchHit(classId, probeId, operandValues);
    }

    /**
     * Records a probe hit that carries no payload — return, throw, or segment
     * probes.
     *
     * @param classId the internal class name
     * @param probeId the probe index within the class
     */
    public static void recordSimpleHit(String classId, int probeId) {
        REGISTRY.current().recordSimpleHit(classId, probeId);
    }

    /**
     * Returns the registry that routes probe-recording calls to the active
     * recorder for the calling thread.
     *
     * @return the per-thread probe recorder registry
     */
    public static ProbeRecorderRegistry registry() {
        return REGISTRY;
    }

    /**
     * Returns the registry that resolves the active test-execution context for
     * the calling thread.
     *
     * @return the per-thread probe context registry
     */
    public static ProbeContextRegistry contextRegistry() {
        return CONTEXT_REGISTRY;
    }
}

package io.github.kd656.coveragex.core.collect;

/**
 * Receives probe-hit events from instrumented bytecode.
 *
 * <p>One method per probe kind mirrors the sealed
 * {@link io.github.kd656.coveragex.api.data.ProbeMetadata} hierarchy.
 * New probe kinds add a new method here rather than a new arm of a
 * switch on a discriminator argument.</p>
 *
 * <p>Default implementations are provided for each method so that
 * existing narrow test doubles that only implement a single kind do not
 * need to be updated.</p>
 */
public interface ProbeRecorder {

    /**
     * Records a method-entry probe hit together with the captured argument
     * values. Only method-entry hits contribute to the per-method invocation
     * report.
     *
     * @param classId    the internal class name
     * @param methodName the simple method name (e.g. {@code doSomething})
     * @param probeId    the probe index within the class
     * @param args       the boxed argument values, never {@code null}
     */
    default void recordMethodEntry(String classId, String methodName,
                                   int probeId, Object[] args) {
    }

    /**
     * Records a branch-direction probe hit together with any operand values
     * that the capture emitter stashed (may be empty).
     *
     * @param classId       the internal class name
     * @param probeId       the probe index within the class
     * @param operandValues the captured operand values, never {@code null}
     */
    default void recordBranchHit(String classId, int probeId, Object[] operandValues) {
    }

    /**
     * Records a probe hit that carries no payload — return, throw, or segment
     * probes.
     *
     * @param classId the internal class name
     * @param probeId the probe index within the class
     */
    default void recordSimpleHit(String classId, int probeId) {
    }
}

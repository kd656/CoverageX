package com.coveragex.core.collect;

/**
 * Receives probe-hit events from instrumented bytecode.
 *
 * <p>This is the narrow hot-path surface: it carries only what the injected
 * {@code recordHit} call needs. Splitting it out from the broader collector
 * interface lets the bytecode-bound delegate depend on the minimum surface
 * and lets test doubles implement only this method without stubbing
 * registration or snapshot APIs they do not exercise.</p>
 */
public interface ProbeRecorder {

    /**
     * Records a probe hit with method name and optional argument data.
     *
     * @param classId    the internal class name
     * @param methodName the simple method name (e.g. {@code doSomething})
     * @param probeId    the probe index within the class
     * @param args       boxed argument values for entry probes, or {@code null} for non-entry probes
     */
    void recordHit(String classId, String methodName, int probeId, Object[] args);
}

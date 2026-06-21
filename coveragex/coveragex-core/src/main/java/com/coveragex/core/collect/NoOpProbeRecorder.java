package com.coveragex.core.collect;

/**
 * Recorder that drops every hit on the floor.
 *
 * <p>Installed as the default global recorder before the agent's premain has
 * a chance to install the real collector — keeps the bytecode-bound
 * {@code recordHit} entry point safe to call at any point in the JVM
 * lifecycle without NPE.</p>
 */
public final class NoOpProbeRecorder implements ProbeRecorder {

    public static final NoOpProbeRecorder INSTANCE = new NoOpProbeRecorder();

    private NoOpProbeRecorder() {}

    @Override
    public void recordHit(String classId, String methodName, int probeId, Object[] args) {
        // intentionally empty
    }
}

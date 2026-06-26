package io.github.kd656.coveragex.core.collect;

/**
 * Recorder that drops every hit on the floor.
 *
 * <p>Installed as the default global recorder before the agent's premain has
 * a chance to install the real collector — keeps the bytecode-bound entry
 * points safe to call at any point in the JVM lifecycle without NPE. All
 * methods are inherited as no-ops from {@link ProbeRecorder}.</p>
 */
public final class NoOpProbeRecorder implements ProbeRecorder {

    public static final NoOpProbeRecorder INSTANCE = new NoOpProbeRecorder();

    private NoOpProbeRecorder() {
    }
}

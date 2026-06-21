package com.coveragex.core.collect;

import com.coveragex.api.data.ExecutionData;
import com.coveragex.api.data.TestTrackingSnapshot;

/**
 * Read-only access to a collector's accumulated coverage state.
 *
 * <p>Used by reporting, test assertions, and the agent's flush path — anywhere
 * that needs to observe what has been collected without contributing new hits
 * or registering classes. Kept separate from {@link ProbeRecorder} and
 * {@link ProbeClassRegistrar} so the read side does not pull recording
 * methods into clients that only consume snapshots.</p>
 */
public interface CoverageStateReader {

    /**
     * Returns a snapshot of all collected coverage data as an {@link ExecutionData} record.
     *
     * <p>The returned snapshot is immutable. Probe arrays are cloned so that subsequent
     * probe hits do not mutate the returned data.</p>
     */
    ExecutionData snapshot();

    /**
     * Returns the probe data for the given class.
     *
     * @param classId the internal class name
     * @return the probe hit array, or {@code null} if not registered
     */
    boolean[] getProbeData(String classId);

    /**
     * Returns a snapshot of the test-attribution tracker associated with this
     * collector.
     *
     * <p>Returns {@link TestTrackingSnapshot#empty()} if the collector has no
     * tracker installed. The real wiring lands in PR 14-E; until then, every
     * implementation returns empty so the interface can be introduced without
     * behavior change.</p>
     */
    TestTrackingSnapshot snapshotTracker();
}

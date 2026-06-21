package com.coveragex.core.collect;

/**
 * Aggregate collector interface — the union of the three role surfaces
 * ({@link ProbeRecorder}, {@link ProbeClassRegistrar}, {@link CoverageStateReader})
 * plus the lifecycle methods ({@link #flush()}, {@link #reset()}, {@link #classCount()})
 * the maven plugin and agent rely on.
 *
 * <p>External embedders should depend on the narrow role interfaces when
 * possible; this aggregate exists for the agent's premain handoff and the
 * report path, which need the full surface in one place.</p>
 */
public interface CoverageDataCollector
        extends ProbeRecorder, ProbeClassRegistrar, CoverageStateReader {

    /**
     * Flushes coverage data to disk.
     */
    void flush();

    /**
     * Clears all collected coverage state.
     */
    void reset();

    /**
     * Returns the total number of registered classes.
     */
    int classCount();
}

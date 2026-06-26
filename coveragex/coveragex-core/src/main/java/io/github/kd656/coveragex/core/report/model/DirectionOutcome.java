package io.github.kd656.coveragex.core.report.model;

/**
 * Coverage outcome for a single direction (TRUE or FALSE) of one operand.
 * The smallest unit of branch coverage data; corresponds one-to-one with a
 * {@link io.github.kd656.coveragex.api.data.ProbeMetadata.BranchProbe}.
 *
 * @param probeId the runtime probe id used to record hits for this direction
 * @param hit     {@code true} if the probe fired at least once
 * @param count   the recorded hit count
 */
public record DirectionOutcome(int probeId, boolean hit, int count) {

    /**
     * Returns an outcome representing a direction that was never taken.
     *
     * @param probeId the probe id assigned to the direction
     * @return a miss outcome with a zero count
     */
    public static DirectionOutcome miss(int probeId) {
        return new DirectionOutcome(probeId, false, 0);
    }
}

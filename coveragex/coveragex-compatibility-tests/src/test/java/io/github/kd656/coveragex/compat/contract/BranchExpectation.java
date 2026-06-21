package io.github.kd656.coveragex.compat.contract;

import io.github.kd656.coveragex.api.data.ProbeMetadata.BranchDirection;

/**
 * Expectation for one {@code BranchProbe} entry in a {@code ProbePlan}.
 *
 * <p>Two precision levels:</p>
 * <ul>
 *   <li>{@link OnLine} — pin the source line and direction. The plan must contain
 *       exactly the expected number of branch probes at that (line, direction)
 *       coordinate (multiset matching in {@link PlanContract}).</li>
 *   <li>{@link AnyLine} — direction only. Used when JDK desugaring legitimately
 *       moves the source line (e.g. {@code switch} fall-through, try-with-resources
 *       synthetic close) but the count and direction of branches is stable.</li>
 * </ul>
 */
public sealed interface BranchExpectation {
    BranchDirection direction();

    record OnLine(int line, BranchDirection direction) implements BranchExpectation {}
    record AnyLine(BranchDirection direction) implements BranchExpectation {}
}

package io.github.kd656.coveragex.compat.contract;

import io.github.kd656.coveragex.api.data.ProbeMetadata;
import io.github.kd656.coveragex.api.data.ProbeMetadata.BranchDirection;
import io.github.kd656.coveragex.api.data.ProbeMetadata.BranchProbe;
import io.github.kd656.coveragex.api.data.ProbeMetadata.MethodProbe;
import io.github.kd656.coveragex.api.data.ProbeMetadata.ReturnProbe;
import io.github.kd656.coveragex.api.data.ProbeMetadata.ThrowProbe;
import io.github.kd656.coveragex.compat.testutil.ProbeMetadataIndex;
import org.assertj.core.api.SoftAssertions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Runtime-hit contract for one fixture.
 *
 * <p>Asserts the *semantics* of what fired during {@code execute()}, resolved
 * via {@link ProbeMetadataIndex}. Single pass over hit IDs.</p>
 */
public record HitsContract(
        int minMethodHits,
        int minReturnHits,
        int minThrowHits,
        boolean requireTrueBranchHit,
        boolean requireFalseBranchHit,
        List<BranchHitExpectation> expectedLinePinnedBranches,
        List<BranchHitExpectation> forbiddenLinePinnedBranches) {

    /**
     * <p>{@code minCount} is the minimum <em>sum of invocation counts</em>
     * across all branch probes matching the (line, direction) pair. With the
     * {@link io.github.kd656.coveragex.api.data.ProbeHit ProbeHit}, this
     * is the per-direction invocation count — e.g. a loop body that runs
     * 3 times has {@code minCount = 3} at the TRUE direction of the loop
     * condition.</p>
     */
    public record BranchHitExpectation(int line, BranchDirection direction, int minCount) {
        public static BranchHitExpectation atLeastOnce(int line, BranchDirection direction) {
            return new BranchHitExpectation(line, direction, 1);
        }
        public static BranchHitExpectation atLeast(int line, BranchDirection direction, int minCount) {
            return new BranchHitExpectation(line, direction, minCount);
        }
    }

    private record BranchKey(int line, BranchDirection direction) {}

    public void verify(Map<Integer, Integer> hitCounts, ProbeMetadataIndex index) {
        int methodHits = 0;
        int returnHits = 0;
        int throwHits = 0;
        boolean sawTrue = false;
        boolean sawFalse = false;
        Map<BranchKey, Integer> branchInvocationSums = new HashMap<>();

        for (Map.Entry<Integer, Integer> e : hitCounts.entrySet()) {
            int id = e.getKey();
            int count = e.getValue();
            if (count <= 0) continue;
            ProbeMetadata m = index.metadataOf(id);
            if (m == null) continue;
            if (m instanceof MethodProbe) {
                methodHits++;
            } else if (m instanceof ReturnProbe) {
                returnHits++;
            } else if (m instanceof ThrowProbe) {
                throwHits++;
            } else if (m instanceof BranchProbe b) {
                if (b.direction() == BranchDirection.TRUE) sawTrue = true;
                else sawFalse = true;
                branchInvocationSums.merge(new BranchKey(b.line(), b.direction()), count, Integer::sum);
            }
        }

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(methodHits).as("method-probe hits").isGreaterThanOrEqualTo(minMethodHits);
        soft.assertThat(returnHits).as("return-probe hits").isGreaterThanOrEqualTo(minReturnHits);
        soft.assertThat(throwHits).as("throw-probe hits").isGreaterThanOrEqualTo(minThrowHits);
        if (requireTrueBranchHit)  soft.assertThat(sawTrue ).as("any TRUE branch hit" ).isTrue();
        if (requireFalseBranchHit) soft.assertThat(sawFalse).as("any FALSE branch hit").isTrue();

        for (BranchHitExpectation e : expectedLinePinnedBranches) {
            int actual = branchInvocationSums.getOrDefault(new BranchKey(e.line(), e.direction()), 0);
            soft.assertThat(actual)
                .as("branch invocations at line %d direction %s", e.line(), e.direction())
                .isGreaterThanOrEqualTo(e.minCount());
        }
        for (BranchHitExpectation f : forbiddenLinePinnedBranches) {
            int actual = branchInvocationSums.getOrDefault(new BranchKey(f.line(), f.direction()), 0);
            soft.assertThat(actual)
                .as("forbidden branch invocations at line %d direction %s", f.line(), f.direction())
                .isEqualTo(0);
        }

        soft.assertAll();
    }

    /** Convenience factory for fixtures with no branches and no throws. */
    public static HitsContract atLeastMethods(int minMethodHits, boolean requireTrue, boolean requireFalse) {
        return new HitsContract(minMethodHits, 0, 0, requireTrue, requireFalse, List.of(), List.of());
    }
}

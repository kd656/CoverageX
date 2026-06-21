package io.github.kd656.coveragex.compat.contract;

import io.github.kd656.coveragex.api.data.ProbeMetadata;
import io.github.kd656.coveragex.api.data.ProbeMetadata.BranchDirection;
import io.github.kd656.coveragex.api.data.ProbeMetadata.BranchProbe;
import io.github.kd656.coveragex.api.data.ProbeMetadata.MethodProbe;
import io.github.kd656.coveragex.api.data.ProbeMetadata.ReturnProbe;
import io.github.kd656.coveragex.api.data.ProbeMetadata.ThrowProbe;
import io.github.kd656.coveragex.core.probe.ProbePlan;
import org.assertj.core.api.SoftAssertions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntPredicate;

/**
 * Static-plan contract for one fixture.
 *
 * <p>Verifies {@code ProbePlanBuilder.build(...)} produces metadata matching the
 * fixture's expected shape, across every supported JDK. Dimensions independently
 * optional — pass zero / empty list / always-true predicate to skip.</p>
 *
 * <p>Single pass over {@code plan.metadata()} categorises probes; subsequent
 * loops iterate small contract-side collections. Line-pinned branches use
 * multiset semantics so duplicate probes at a pinned line fail the assertion.</p>
 */
public record PlanContract(
        int expectedMethodProbes,
        List<BranchExpectation> branches,
        int expectedReturnCount,
        IntPredicate returnLineCheck,
        int expectedThrowCount,
        IntPredicate throwLineCheck) {

    private record BranchKey(int line, BranchDirection direction) {}

    public void verify(ProbePlan plan) {
        int methodCount = 0;
        int actualTrueBranches = 0;
        int actualFalseBranches = 0;
        Map<BranchKey, Integer> pinnedBranchPool = new HashMap<>();
        List<ReturnProbe> returnProbes = new ArrayList<>();
        List<ThrowProbe> throwProbes = new ArrayList<>();

        for (ProbeMetadata m : plan.metadata()) {
            if (m instanceof MethodProbe) {
                methodCount++;
            } else if (m instanceof BranchProbe b) {
                if (b.direction() == BranchDirection.TRUE) actualTrueBranches++;
                else actualFalseBranches++;
                pinnedBranchPool.merge(new BranchKey(b.line(), b.direction()), 1, Integer::sum);
            } else if (m instanceof ReturnProbe r) {
                returnProbes.add(r);
            } else if (m instanceof ThrowProbe t) {
                throwProbes.add(t);
            }
            // SegmentProbe + LineProbe-derived: not consumed by contracts today
        }

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(methodCount).as("method probes").isEqualTo(expectedMethodProbes);

        if (expectedReturnCount > 0) {
            soft.assertThat(returnProbes.size())
                .as("return probes (exact)")
                .isEqualTo(expectedReturnCount);
        }
        for (ReturnProbe r : returnProbes) {
            soft.assertThat(returnLineCheck.test(r.line()))
                .as("return probe line %d is allowed", r.line())
                .isTrue();
        }

        if (expectedThrowCount > 0) {
            soft.assertThat(throwProbes.size())
                .as("throw probes (exact)")
                .isEqualTo(expectedThrowCount);
        }
        for (ThrowProbe t : throwProbes) {
            soft.assertThat(throwLineCheck.test(t.line()))
                .as("throw probe line %d is allowed", t.line())
                .isTrue();
        }

        // Line-pinned branches: multiset consumption (catches duplicates).
        int expectedAnyTrue = 0;
        int expectedAnyFalse = 0;
        Set<BranchKey> pinnedKeys = new HashSet<>();
        for (BranchExpectation expect : branches) {
            if (expect instanceof BranchExpectation.OnLine onLine) {
                BranchKey k = new BranchKey(onLine.line(), onLine.direction());
                pinnedKeys.add(k);
                Integer remaining = pinnedBranchPool.get(k);
                if (remaining == null || remaining == 0) {
                    soft.fail("branch at line %d with direction %s — none in plan",
                            onLine.line(), onLine.direction());
                } else {
                    pinnedBranchPool.put(k, remaining - 1);
                }
            } else if (expect instanceof BranchExpectation.AnyLine anyLine) {
                if (anyLine.direction() == BranchDirection.TRUE) expectedAnyTrue++;
                else expectedAnyFalse++;
            }
        }
        for (Map.Entry<BranchKey, Integer> e : pinnedBranchPool.entrySet()) {
            if (pinnedKeys.contains(e.getKey()) && e.getValue() > 0) {
                soft.fail("unexpected extra %d branch probe(s) at line %d direction %s",
                        e.getValue(), e.getKey().line(), e.getKey().direction());
            }
        }

        if (expectedAnyTrue > 0) {
            soft.assertThat(actualTrueBranches)
                .as("TRUE branches (any-line expectations)")
                .isGreaterThanOrEqualTo(expectedAnyTrue);
        }
        if (expectedAnyFalse > 0) {
            soft.assertThat(actualFalseBranches)
                .as("FALSE branches (any-line expectations)")
                .isGreaterThanOrEqualTo(expectedAnyFalse);
        }

        soft.assertAll();
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private int methods = 1;
        private final List<BranchExpectation> branches = new ArrayList<>();
        private int returnCount = 0;
        private IntPredicate returnCheck = line -> true;
        private int throwCount = 0;
        private IntPredicate throwCheck = line -> true;

        public Builder methodProbes(int n) { this.methods = n; return this; }

        public Builder branch(int line, BranchDirection direction) {
            branches.add(new BranchExpectation.OnLine(line, direction));
            return this;
        }

        public Builder anyLineBranch(BranchDirection direction) {
            branches.add(new BranchExpectation.AnyLine(direction));
            return this;
        }

        /**
         * Pin the allowed return-probe lines. Implicitly sets the exact return count
         * to the number of distinct lines unless overridden by {@link #returnCount(int)}.
         */
        public Builder returnsOnLines(int... lines) {
            Set<Integer> allowed = new HashSet<>();
            for (int l : lines) allowed.add(l);
            this.returnCheck = allowed::contains;
            if (returnCount == 0) returnCount = lines.length;
            return this;
        }

        public Builder returnCount(int n) { this.returnCount = n; return this; }

        public Builder throwsOnLines(int... lines) {
            Set<Integer> allowed = new HashSet<>();
            for (int l : lines) allowed.add(l);
            this.throwCheck = allowed::contains;
            if (throwCount == 0) throwCount = lines.length;
            return this;
        }

        public Builder throwCount(int n) { this.throwCount = n; return this; }

        public PlanContract build() {
            return new PlanContract(
                    methods, List.copyOf(branches),
                    returnCount, returnCheck,
                    throwCount, throwCheck);
        }
    }
}

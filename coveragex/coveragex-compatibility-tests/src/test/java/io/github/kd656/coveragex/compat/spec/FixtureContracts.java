package io.github.kd656.coveragex.compat.spec;

import io.github.kd656.coveragex.compat.contract.ArgsContract;
import io.github.kd656.coveragex.compat.contract.HitsContract;
import io.github.kd656.coveragex.compat.contract.InvocationContract;
import io.github.kd656.coveragex.compat.contract.PlanContract;
import io.github.kd656.coveragex.compat.contract.TestAttributionContract;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Bundle of optional contract slots for one fixture.
 *
 * <p>Each slot represents one verification dimension. A spec either declares
 * a real contract for the slot or explicitly opts out with
 * {@code .skipX(reason)}. {@link Builder#build()} throws if any slot is left
 * untouched — silence is not a valid stance, because future contract
 * dimensions would silently expand and existing specs would assert nothing
 * on the new axis.</p>
 */
public record FixtureContracts(
        Optional<PlanContract> plan,
        Optional<HitsContract> hits,
        Optional<ArgsContract> args,
        Optional<InvocationContract> invocations,
        Optional<TestAttributionContract> attribution,
        Map<Slot, String> skipReasons) {

    public FixtureContracts {
        skipReasons = Map.copyOf(skipReasons);
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Enumerates the contract dimensions the builder tracks. */
    public enum Slot { PLAN, HITS, ARGS, INVOCATIONS, ATTRIBUTION }

    public static final class Builder {

        private PlanContract plan;
        private HitsContract hits;
        private ArgsContract args;
        private InvocationContract invocations;
        private TestAttributionContract attribution;
        private final EnumMap<Slot, Boolean> touched = new EnumMap<>(Slot.class);
        private final EnumMap<Slot, String> skipReasons = new EnumMap<>(Slot.class);

        public Builder plan(PlanContract contract) {
            this.plan = contract;
            this.touched.put(Slot.PLAN, true);
            return this;
        }

        public Builder skipPlan(String reason) {
            this.skipReasons.put(Slot.PLAN, requireReason(reason));
            this.touched.put(Slot.PLAN, true);
            return this;
        }

        public Builder hits(HitsContract contract) {
            this.hits = contract;
            this.touched.put(Slot.HITS, true);
            return this;
        }

        public Builder skipHits(String reason) {
            this.skipReasons.put(Slot.HITS, requireReason(reason));
            this.touched.put(Slot.HITS, true);
            return this;
        }

        public Builder args(ArgsContract contract) {
            this.args = contract;
            this.touched.put(Slot.ARGS, true);
            return this;
        }

        public Builder skipArgs(String reason) {
            this.skipReasons.put(Slot.ARGS, requireReason(reason));
            this.touched.put(Slot.ARGS, true);
            return this;
        }

        public Builder invocations(InvocationContract contract) {
            this.invocations = contract;
            this.touched.put(Slot.INVOCATIONS, true);
            return this;
        }

        public Builder skipInvocations(String reason) {
            this.skipReasons.put(Slot.INVOCATIONS, requireReason(reason));
            this.touched.put(Slot.INVOCATIONS, true);
            return this;
        }

        public Builder attribution(TestAttributionContract contract) {
            this.attribution = contract;
            this.touched.put(Slot.ATTRIBUTION, true);
            return this;
        }

        public Builder skipAttribution(String reason) {
            this.skipReasons.put(Slot.ATTRIBUTION, requireReason(reason));
            this.touched.put(Slot.ATTRIBUTION, true);
            return this;
        }

        public FixtureContracts build() {
            for (Slot s : Slot.values()) {
                if (!touched.getOrDefault(s, false)) {
                    throw new IllegalStateException(
                            "Fixture spec must declare a position on " + s
                                    + " — call the dimension method with a contract, or skip"
                                    + s.name().charAt(0) + s.name().substring(1).toLowerCase()
                                    + "(reason) to explicitly opt out.");
                }
            }
            return new FixtureContracts(
                    Optional.ofNullable(plan),
                    Optional.ofNullable(hits),
                    Optional.ofNullable(args),
                    Optional.ofNullable(invocations),
                    Optional.ofNullable(attribution),
                    skipReasons);
        }

        private static String requireReason(String reason) {
            if (reason == null || reason.isBlank()) {
                throw new IllegalArgumentException("skip reason must be a non-blank string");
            }
            return reason;
        }
    }
}

package com.coveragex.compat.spec;

import com.coveragex.compat.contract.ArgsContract;
import com.coveragex.compat.contract.HitsContract;
import com.coveragex.compat.contract.InvocationContract;
import com.coveragex.compat.contract.PlanContract;
import com.coveragex.compat.contract.TestAttributionContract;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FixtureContractsBuilderTest {

    @Test
    void buildFailsWhenAnyDimensionIsUntouched() {
        assertThatThrownBy(() -> FixtureContracts.builder().build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PLAN");

        assertThatThrownBy(() -> FixtureContracts.builder().plan(stubPlan()).build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HITS");

        assertThatThrownBy(() -> FixtureContracts.builder()
                .plan(stubPlan()).hits(stubHits()).build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ARGS");

        assertThatThrownBy(() -> FixtureContracts.builder()
                .plan(stubPlan()).hits(stubHits())
                .args(stubArgs()).build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("INVOCATIONS");

        assertThatThrownBy(() -> FixtureContracts.builder()
                .plan(stubPlan()).hits(stubHits())
                .args(stubArgs()).invocations(stubInvocations()).build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ATTRIBUTION");
    }

    @Test
    void realContractSatisfiesDimension() {
        FixtureContracts result = FixtureContracts.builder()
                .plan(stubPlan())
                .hits(stubHits())
                .args(stubArgs())
                .invocations(stubInvocations())
                .attribution(stubAttribution())
                .build();

        assertThat(result.plan()).isPresent();
        assertThat(result.hits()).isPresent();
        assertThat(result.args()).isPresent();
        assertThat(result.invocations()).isPresent();
        assertThat(result.attribution()).isPresent();
        assertThat(result.skipReasons()).isEmpty();
    }

    @Test
    void skipReasonSatisfiesDimensionWithoutContractPresence() {
        FixtureContracts result = FixtureContracts.builder()
                .plan(stubPlan())
                .skipHits("fixture is static-init only — no runtime hits to verify")
                .skipArgs("no args to pin")
                .skipInvocations("no invocation counts to pin")
                .skipAttribution("no per-test scenario")
                .build();

        assertThat(result.plan()).isPresent();
        assertThat(result.hits()).isEmpty();
        assertThat(result.skipReasons())
                .containsEntry(FixtureContracts.Slot.HITS, "fixture is static-init only — no runtime hits to verify")
                .containsEntry(FixtureContracts.Slot.ARGS, "no args to pin")
                .containsEntry(FixtureContracts.Slot.INVOCATIONS, "no invocation counts to pin")
                .containsEntry(FixtureContracts.Slot.ATTRIBUTION, "no per-test scenario");
    }

    @Test
    void skipRejectsBlankReason() {
        assertThatThrownBy(() -> FixtureContracts.builder().skipPlan(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FixtureContracts.builder().skipHits(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static PlanContract stubPlan() {
        return PlanContract.builder().methodProbes(1).build();
    }

    private static HitsContract stubHits() {
        return new HitsContract(0, 0, 0, false, false, List.of(), List.of());
    }

    private static ArgsContract stubArgs() {
        return ArgsContract.builder().build();
    }

    private static InvocationContract stubInvocations() {
        return InvocationContract.builder().build();
    }

    private static TestAttributionContract stubAttribution() {
        return TestAttributionContract.builder().build();
    }
}

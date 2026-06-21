package com.coveragex.compat.spec.fixture;

import com.coveragex.compat.contract.HitsContract;
import com.coveragex.compat.contract.PlanContract;
import com.coveragex.compat.spec.FixtureContractSpec;
import com.coveragex.compat.spec.FixtureContracts;
import com.coveragex.compat.contract.ArgsContract;
import com.coveragex.compat.contract.TestAttributionContract;
import com.coveragex.compat.spec.InvocationStep;
import java.util.List;

/**
 * Spec for {@code com.coveragex.fixtures.SwitchString}.
 *
 * <p>Migrated from the legacy {@code SwitchStringContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class SwitchStringSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "com.coveragex.fixtures.SwitchString";
    }

    @Override
    public FixtureContracts contracts() {
        return FixtureContracts.builder()
                .plan(PlanContract.builder().methodProbes(3).build())
                .hits(HitsContract.atLeastMethods(2, false, false))
                .args(ArgsContract.builder()
                        .method("order", List.of(List.of("a"), List.of("b"), List.of("z")))
                        .build())
                .skipInvocations("each case hit once")
                .attribution(TestAttributionContract.builder()
                        .method("order", "SwitchString#a", "SwitchString#b", "SwitchString#default")
                        .build())
                .build();

    }

    @Override
    public List<InvocationStep> invocationPlan() {
        return List.of(
                InvocationStep.of("order", List.of("a"), "SwitchString#a"),
                InvocationStep.of("order", List.of("b"), "SwitchString#b"),
                InvocationStep.of("order", List.of("z"), "SwitchString#default"));
    }
}

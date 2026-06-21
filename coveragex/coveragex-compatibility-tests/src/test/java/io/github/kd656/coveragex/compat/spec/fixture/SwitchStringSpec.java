package io.github.kd656.coveragex.compat.spec.fixture;

import io.github.kd656.coveragex.compat.contract.HitsContract;
import io.github.kd656.coveragex.compat.contract.PlanContract;
import io.github.kd656.coveragex.compat.spec.FixtureContractSpec;
import io.github.kd656.coveragex.compat.spec.FixtureContracts;
import io.github.kd656.coveragex.compat.contract.ArgsContract;
import io.github.kd656.coveragex.compat.contract.TestAttributionContract;
import io.github.kd656.coveragex.compat.spec.InvocationStep;
import java.util.List;

/**
 * Spec for {@code io.github.kd656.coveragex.fixtures.SwitchString}.
 *
 * <p>Migrated from the legacy {@code SwitchStringContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class SwitchStringSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "io.github.kd656.coveragex.fixtures.SwitchString";
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

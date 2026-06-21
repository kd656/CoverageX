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
 * Spec for {@code io.github.kd656.coveragex.fixtures.StaticFns}.
 *
 * <p>Migrated from the legacy {@code StaticFnsContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class StaticFnsSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "io.github.kd656.coveragex.fixtures.StaticFns";
    }

    @Override
    public FixtureContracts contracts() {
        return FixtureContracts.builder()
                .plan(PlanContract.builder().methodProbes(4).build())
                .hits(HitsContract.atLeastMethods(3, false, false))
                .args(ArgsContract.builder()
                        .method("add", List.of(List.of("2", "3")))
                        .method("square", List.of(List.of("4")))
                        .build())
                .skipInvocations("each method called once; hits already covers firing")
                .attribution(TestAttributionContract.builder()
                        .method("add", "StaticFns#add")
                        .method("square", "StaticFns#square")
                        .build())
                .build();

    }

    @Override
    public List<InvocationStep> invocationPlan() {
        return List.of(
                InvocationStep.of("add", List.of(2, 3), "StaticFns#add"),
                InvocationStep.of("square", List.of(4), "StaticFns#square"));
    }
}

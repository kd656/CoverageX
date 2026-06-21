package io.github.kd656.coveragex.compat.spec.fixture;

import io.github.kd656.coveragex.compat.contract.HitsContract;
import io.github.kd656.coveragex.compat.contract.PlanContract;
import io.github.kd656.coveragex.compat.spec.FixtureContractSpec;
import io.github.kd656.coveragex.compat.spec.FixtureContracts;
import io.github.kd656.coveragex.compat.contract.TestAttributionContract;
import io.github.kd656.coveragex.compat.spec.InvocationStep;
import java.util.List;

/**
 * Spec for {@code io.github.kd656.coveragex.fixtures.ReturnPrim}.
 *
 * <p>Migrated from the legacy {@code ReturnPrimContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class ReturnPrimSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "io.github.kd656.coveragex.fixtures.ReturnPrim";
    }

    @Override
    public FixtureContracts contracts() {
        return FixtureContracts.builder()
                .plan(PlanContract.builder().methodProbes(5).build())
                .hits(HitsContract.atLeastMethods(4, false, false))
                .skipArgs("asInt / asLong / asDouble are all no-arg returns")
                .skipInvocations("each return method called once")
                .attribution(TestAttributionContract.builder()
                        .method("asInt", "ReturnPrim#asInt")
                        .method("asLong", "ReturnPrim#asLong")
                        .method("asDouble", "ReturnPrim#asDouble")
                        .build())
                .build();

    }

    @Override
    public List<InvocationStep> invocationPlan() {
        return List.of(
                InvocationStep.of("asInt", List.of(), "ReturnPrim#asInt"),
                InvocationStep.of("asLong", List.of(), "ReturnPrim#asLong"),
                InvocationStep.of("asDouble", List.of(), "ReturnPrim#asDouble"));
    }
}

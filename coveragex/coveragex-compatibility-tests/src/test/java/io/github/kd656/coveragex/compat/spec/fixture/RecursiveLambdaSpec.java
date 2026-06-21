package io.github.kd656.coveragex.compat.spec.fixture;

import io.github.kd656.coveragex.compat.contract.HitsContract;
import io.github.kd656.coveragex.compat.contract.PlanContract;
import io.github.kd656.coveragex.compat.spec.FixtureContractSpec;
import io.github.kd656.coveragex.compat.spec.FixtureContracts;

/**
 * Spec for {@code io.github.kd656.coveragex.fixtures.RecursiveLambda}.
 *
 * <p>Migrated from the legacy {@code RecursiveLambdaContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class RecursiveLambdaSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "io.github.kd656.coveragex.fixtures.RecursiveLambda";
    }

    @Override
    public FixtureContracts contracts() {
        return FixtureContracts.builder()
                .plan(PlanContract.builder().methodProbes(4).build())
                .hits(HitsContract.atLeastMethods(2, false, false))
                .skipArgs("Function.apply args ride through invokedynamic; recorded probe args are at the synthetic level")
                .skipInvocations("the recursive synthetic method name varies by JDK; pinning deferred until first-run discovery confirms the name")
                .skipAttribution("single outer execution; recursive lambda entries cannot be attributed to different test contexts without nested scope reopen")
                .build();

    }
}

package io.github.kd656.coveragex.compat.spec.fixture;

import io.github.kd656.coveragex.compat.contract.HitsContract;
import io.github.kd656.coveragex.compat.contract.PlanContract;
import io.github.kd656.coveragex.compat.spec.FixtureContractSpec;
import io.github.kd656.coveragex.compat.spec.FixtureContracts;

/**
 * Spec for {@code io.github.kd656.coveragex.fixtures.TryInLambda}.
 *
 * <p>Migrated from the legacy {@code TryInLambdaContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class TryInLambdaSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "io.github.kd656.coveragex.fixtures.TryInLambda";
    }

    @Override
    public FixtureContracts contracts() {
        return FixtureContracts.builder()
                .plan(PlanContract.builder().methodProbes(4).build())
                .hits(HitsContract.atLeastMethods(2, false, false))
                .skipArgs("synthetic lambda methods; no user-visible args")
                .skipInvocations("each lambda once")
                .skipAttribution("two parallel synthetic methods; splitting them under labels does not carry signal")
                .build();

    }
}

package io.github.kd656.coveragex.compat.spec.fixture;

import io.github.kd656.coveragex.compat.contract.HitsContract;
import io.github.kd656.coveragex.compat.contract.PlanContract;
import io.github.kd656.coveragex.compat.spec.FixtureContractSpec;
import io.github.kd656.coveragex.compat.spec.FixtureContracts;

/**
 * Spec for {@code io.github.kd656.coveragex.fixtures.AnonInLambda}.
 *
 * <p>Migrated from the legacy {@code AnonInLambdaContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class AnonInLambdaSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "io.github.kd656.coveragex.fixtures.AnonInLambda";
    }

    @Override
    public FixtureContracts contracts() {
        return FixtureContracts.builder()
                .plan(PlanContract.builder().methodProbes(3).build())
                .hits(HitsContract.atLeastMethods(2, false, false))
                .skipArgs("synthetic supplier + anonymous-class run; no user args")
                .skipInvocations("trivial")
                .skipAttribution("single chained execution")
                .build();

    }
}

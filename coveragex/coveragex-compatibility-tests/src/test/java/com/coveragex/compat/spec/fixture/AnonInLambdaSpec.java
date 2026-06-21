package com.coveragex.compat.spec.fixture;

import com.coveragex.compat.contract.HitsContract;
import com.coveragex.compat.contract.PlanContract;
import com.coveragex.compat.spec.FixtureContractSpec;
import com.coveragex.compat.spec.FixtureContracts;

/**
 * Spec for {@code com.coveragex.fixtures.AnonInLambda}.
 *
 * <p>Migrated from the legacy {@code AnonInLambdaContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class AnonInLambdaSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "com.coveragex.fixtures.AnonInLambda";
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

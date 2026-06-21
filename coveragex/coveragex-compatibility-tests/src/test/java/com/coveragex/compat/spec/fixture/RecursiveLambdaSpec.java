package com.coveragex.compat.spec.fixture;

import com.coveragex.compat.contract.HitsContract;
import com.coveragex.compat.contract.PlanContract;
import com.coveragex.compat.spec.FixtureContractSpec;
import com.coveragex.compat.spec.FixtureContracts;

/**
 * Spec for {@code com.coveragex.fixtures.RecursiveLambda}.
 *
 * <p>Migrated from the legacy {@code RecursiveLambdaContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class RecursiveLambdaSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "com.coveragex.fixtures.RecursiveLambda";
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

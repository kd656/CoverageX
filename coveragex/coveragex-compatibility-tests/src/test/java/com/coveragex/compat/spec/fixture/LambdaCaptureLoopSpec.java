package com.coveragex.compat.spec.fixture;

import com.coveragex.compat.contract.HitsContract;
import com.coveragex.compat.contract.PlanContract;
import com.coveragex.compat.spec.FixtureContractSpec;
import com.coveragex.compat.spec.FixtureContracts;

/**
 * Spec for {@code com.coveragex.fixtures.LambdaCaptureLoop}.
 *
 * <p>Migrated from the legacy {@code LambdaCaptureLoopContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class LambdaCaptureLoopSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "com.coveragex.fixtures.LambdaCaptureLoop";
    }

    @Override
    public FixtureContracts contracts() {
        return FixtureContracts.builder()
                .plan(PlanContract.builder().methodProbes(4).build())
                .hits(HitsContract.atLeastMethods(2, false, false))
                .skipArgs("no outer args; captured lambdas have synthetic args")
                .skipInvocations("single outer call; individual lambda invocations are exercised by hits")
                .skipAttribution("single outer execution")
                .build();

    }
}

package com.coveragex.compat.spec.fixture;

import com.coveragex.compat.contract.HitsContract;
import com.coveragex.compat.contract.PlanContract;
import com.coveragex.compat.spec.FixtureContractSpec;
import com.coveragex.compat.spec.FixtureContracts;

/**
 * Spec for {@code com.coveragex.fixtures.Lambda}.
 *
 * <p>Migrated from the legacy {@code LambdaContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class LambdaSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "com.coveragex.fixtures.Lambda";
    }

    @Override
    public FixtureContracts contracts() {
        return FixtureContracts.builder()
                .plan(PlanContract.builder()
                .methodProbes(3)   // <init> + execute + synthetic lambda$execute$0
                .build())
                .hits(HitsContract.atLeastMethods(2, false, false))
                .skipArgs("lambda body is a synthetic method with no user-meaningful args")
                .skipInvocations("single lambda invocation")
                .skipAttribution("single execution")
                .build();

    }
}

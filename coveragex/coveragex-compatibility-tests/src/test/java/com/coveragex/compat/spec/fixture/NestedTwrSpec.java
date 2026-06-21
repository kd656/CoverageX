package com.coveragex.compat.spec.fixture;

import com.coveragex.compat.contract.HitsContract;
import com.coveragex.compat.contract.PlanContract;
import com.coveragex.compat.spec.FixtureContractSpec;
import com.coveragex.compat.spec.FixtureContracts;

/**
 * Spec for {@code com.coveragex.fixtures.NestedTwr}.
 *
 * <p>Migrated from the legacy {@code NestedTwrContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class NestedTwrSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "com.coveragex.fixtures.NestedTwr";
    }

    @Override
    public FixtureContracts contracts() {
        return FixtureContracts.builder()
                .plan(PlanContract.builder().methodProbes(3).build())
                .hits(HitsContract.atLeastMethods(2, false, false))
                .skipArgs("doWork() takes no arguments")
                .skipInvocations("single call")
                .skipAttribution("single execution")
                .build();

    }
}

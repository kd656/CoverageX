package com.coveragex.compat.spec.fixture;

import com.coveragex.compat.contract.HitsContract;
import com.coveragex.compat.contract.PlanContract;
import com.coveragex.compat.spec.FixtureContractSpec;
import com.coveragex.compat.spec.FixtureContracts;

/**
 * Spec for {@code com.coveragex.fixtures.MethodRef}.
 *
 * <p>Migrated from the legacy {@code MethodRefContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class MethodRefSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "com.coveragex.fixtures.MethodRef";
    }

    @Override
    public FixtureContracts contracts() {
        return FixtureContracts.builder()
                .plan(PlanContract.builder().methodProbes(2).build())
                .hits(HitsContract.atLeastMethods(1, false, false))
                .skipArgs("method-ref synthetic; captured args are implementation detail")
                .skipInvocations("single application")
                .skipAttribution("single execution")
                .build();

    }
}

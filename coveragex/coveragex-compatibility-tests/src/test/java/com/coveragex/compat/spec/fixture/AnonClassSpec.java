package com.coveragex.compat.spec.fixture;

import com.coveragex.compat.contract.HitsContract;
import com.coveragex.compat.contract.PlanContract;
import com.coveragex.compat.spec.FixtureContractSpec;
import com.coveragex.compat.spec.FixtureContracts;

/**
 * Spec for {@code com.coveragex.fixtures.AnonClass}.
 *
 * <p>Migrated from the legacy {@code AnonClassContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class AnonClassSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "com.coveragex.fixtures.AnonClass";
    }

    @Override
    public FixtureContracts contracts() {
        return FixtureContracts.builder()
                .plan(PlanContract.builder().methodProbes(2).build())
                .hits(HitsContract.atLeastMethods(1, false, false))
                .skipArgs("no user-method args; synthetic Outer$1.run")
                .skipInvocations("single invocation")
                .skipAttribution("single execution")
                .build();

    }
}

package com.coveragex.compat.spec.fixture;

import com.coveragex.compat.contract.HitsContract;
import com.coveragex.compat.contract.PlanContract;
import com.coveragex.compat.spec.FixtureContractSpec;
import com.coveragex.compat.spec.FixtureContracts;

/**
 * Spec for {@code com.coveragex.fixtures.CtorDefault}.
 *
 * <p>Migrated from the legacy {@code CtorDefaultContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class CtorDefaultSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "com.coveragex.fixtures.CtorDefault";
    }

    @Override
    public FixtureContracts contracts() {
        return FixtureContracts.builder()
                .plan(PlanContract.builder().methodProbes(2).build())
                .hits(HitsContract.atLeastMethods(2, false, false))
                .skipArgs("synthetic no-arg ctor — nothing to capture")
                .skipInvocations("single construction")
                .skipAttribution("single ctor call, no per-test value")
                .build();

    }
}

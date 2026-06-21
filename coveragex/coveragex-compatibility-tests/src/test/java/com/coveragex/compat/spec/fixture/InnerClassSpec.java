package com.coveragex.compat.spec.fixture;

import com.coveragex.compat.contract.HitsContract;
import com.coveragex.compat.contract.PlanContract;
import com.coveragex.compat.spec.FixtureContractSpec;
import com.coveragex.compat.spec.FixtureContracts;

/**
 * Spec for {@code com.coveragex.fixtures.InnerClass}.
 *
 * <p>Migrated from the legacy {@code InnerClassContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class InnerClassSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "com.coveragex.fixtures.InnerClass";
    }

    @Override
    public FixtureContracts contracts() {
        return FixtureContracts.builder()
                .plan(PlanContract.builder().methodProbes(2).build())
                .hits(HitsContract.atLeastMethods(1, false, false))
                .skipArgs("doubled() lives on nested class Inner; runner currently inspects only the outer class's methodHits")
                .skipInvocations("single call")
                .skipAttribution("single execution")
                .build();

    }
}

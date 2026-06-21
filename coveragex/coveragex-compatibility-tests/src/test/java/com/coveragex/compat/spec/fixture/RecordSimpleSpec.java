package com.coveragex.compat.spec.fixture;

import com.coveragex.compat.contract.HitsContract;
import com.coveragex.compat.contract.PlanContract;
import com.coveragex.compat.spec.FixtureContractSpec;
import com.coveragex.compat.spec.FixtureContracts;

/**
 * Spec for {@code com.coveragex.fixtures.RecordSimple}.
 *
 * <p>Migrated from the legacy {@code RecordSimpleContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class RecordSimpleSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "com.coveragex.fixtures.RecordSimple";
    }

    @Override
    public FixtureContracts contracts() {
        return FixtureContracts.builder()
                .plan(PlanContract.builder().methodProbes(2).build())
                .hits(HitsContract.atLeastMethods(1, false, false))
                .skipArgs("Point<init> is on the nested record class; runner currently inspects only the outer class's methodHits")
                .skipInvocations("composite execution; counts trivial per accessor")
                .skipAttribution("composite execution — splitting accessor calls under labels is cosmetic")
                .build();

    }
}

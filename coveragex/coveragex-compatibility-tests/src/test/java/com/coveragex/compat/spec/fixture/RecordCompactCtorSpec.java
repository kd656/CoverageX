package com.coveragex.compat.spec.fixture;

import com.coveragex.compat.contract.HitsContract;
import com.coveragex.compat.contract.PlanContract;
import com.coveragex.compat.spec.FixtureContractSpec;
import com.coveragex.compat.spec.FixtureContracts;

/**
 * Spec for {@code com.coveragex.fixtures.RecordCompactCtor}.
 *
 * <p>Migrated from the legacy {@code RecordCompactCtorContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class RecordCompactCtorSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "com.coveragex.fixtures.RecordCompactCtor";
    }

    @Override
    public FixtureContracts contracts() {
        return FixtureContracts.builder()
                .plan(PlanContract.builder().methodProbes(2).build())
                .hits(HitsContract.atLeastMethods(1, false, false))
                .skipArgs("Positive<init> is on the nested record class; runner currently inspects only the outer class's methodHits")
                .skipInvocations("each ctor call once")
                .skipAttribution("requires the runner to reflectively call Positive(int) via Constructor.newInstance; deferred until runner gains ctor-reflection support")
                .build();

    }
}

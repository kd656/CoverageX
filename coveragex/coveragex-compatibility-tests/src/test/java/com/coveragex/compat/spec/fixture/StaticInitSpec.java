package com.coveragex.compat.spec.fixture;

import com.coveragex.compat.contract.HitsContract;
import com.coveragex.compat.contract.PlanContract;
import com.coveragex.compat.spec.FixtureContractSpec;
import com.coveragex.compat.spec.FixtureContracts;

/**
 * Spec for {@code com.coveragex.fixtures.StaticInit}.
 *
 * <p>Migrated from the legacy {@code StaticInitContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class StaticInitSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "com.coveragex.fixtures.StaticInit";
    }

    @Override
    public FixtureContracts contracts() {
        return FixtureContracts.builder()
                .plan(PlanContract.builder().methodProbes(4).build())
                .hits(HitsContract.atLeastMethods(3, false, false))
                .skipArgs("<clinit> and get() both no-arg")
                .skipInvocations("class-load only")
                .skipAttribution("<clinit> runs at class-load before any test scope")
                .build();

    }
}

package com.coveragex.compat.spec.fixture;

import com.coveragex.compat.contract.HitsContract;
import com.coveragex.compat.contract.PlanContract;
import com.coveragex.compat.spec.FixtureContractSpec;
import com.coveragex.compat.spec.FixtureContracts;

/**
 * Spec for {@code com.coveragex.fixtures.StaticInitTryCatch}.
 *
 * <p>Migrated from the legacy {@code StaticInitTryCatchContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class StaticInitTryCatchSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "com.coveragex.fixtures.StaticInitTryCatch";
    }

    @Override
    public FixtureContracts contracts() {
        return FixtureContracts.builder()
                .plan(PlanContract.builder().methodProbes(4).build())
                .hits(HitsContract.atLeastMethods(2, false, false))
                .skipArgs("<clinit> takes no arguments; get() no-arg")
                .skipInvocations("class-load only")
                .skipAttribution("<clinit> runs before any test scope can sensibly attribute it")
                .build();

    }
}

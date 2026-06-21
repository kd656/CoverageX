package com.coveragex.compat.spec.fixture;

import com.coveragex.compat.contract.HitsContract;
import com.coveragex.compat.contract.PlanContract;
import com.coveragex.compat.spec.FixtureContractSpec;
import com.coveragex.compat.spec.FixtureContracts;

/**
 * Spec for {@code com.coveragex.fixtures.VoidFn}.
 *
 * <p>Migrated from the legacy {@code VoidFnContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class VoidFnSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "com.coveragex.fixtures.VoidFn";
    }

    @Override
    public FixtureContracts contracts() {
        return FixtureContracts.builder()
                .plan(PlanContract.builder()
                .methodProbes(3)   // <init> + doNothing + execute
                .build())
                .hits(HitsContract.atLeastMethods(2, false, false))
                .skipArgs("doNothing() takes no arguments")
                .skipInvocations("trivially 1 — hits already proves it fired")
                .skipAttribution("single method, single call — nothing to split")
                .build();

    }
}

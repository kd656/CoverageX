package io.github.kd656.coveragex.compat.spec.fixture;

import io.github.kd656.coveragex.compat.contract.HitsContract;
import io.github.kd656.coveragex.compat.contract.PlanContract;
import io.github.kd656.coveragex.compat.spec.FixtureContractSpec;
import io.github.kd656.coveragex.compat.spec.FixtureContracts;

/**
 * Spec for {@code io.github.kd656.coveragex.fixtures.VoidFn}.
 *
 * <p>Migrated from the legacy {@code VoidFnContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class VoidFnSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "io.github.kd656.coveragex.fixtures.VoidFn";
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

package io.github.kd656.coveragex.compat.spec.fixture;

import io.github.kd656.coveragex.compat.contract.HitsContract;
import io.github.kd656.coveragex.compat.contract.PlanContract;
import io.github.kd656.coveragex.compat.spec.FixtureContractSpec;
import io.github.kd656.coveragex.compat.spec.FixtureContracts;

/**
 * Spec for {@code io.github.kd656.coveragex.fixtures.CtorDefault}.
 *
 * <p>Migrated from the legacy {@code CtorDefaultContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class CtorDefaultSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "io.github.kd656.coveragex.fixtures.CtorDefault";
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

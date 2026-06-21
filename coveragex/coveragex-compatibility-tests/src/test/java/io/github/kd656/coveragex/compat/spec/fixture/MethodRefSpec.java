package io.github.kd656.coveragex.compat.spec.fixture;

import io.github.kd656.coveragex.compat.contract.HitsContract;
import io.github.kd656.coveragex.compat.contract.PlanContract;
import io.github.kd656.coveragex.compat.spec.FixtureContractSpec;
import io.github.kd656.coveragex.compat.spec.FixtureContracts;

/**
 * Spec for {@code io.github.kd656.coveragex.fixtures.MethodRef}.
 *
 * <p>Migrated from the legacy {@code MethodRefContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class MethodRefSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "io.github.kd656.coveragex.fixtures.MethodRef";
    }

    @Override
    public FixtureContracts contracts() {
        return FixtureContracts.builder()
                .plan(PlanContract.builder().methodProbes(2).build())
                .hits(HitsContract.atLeastMethods(1, false, false))
                .skipArgs("method-ref synthetic; captured args are implementation detail")
                .skipInvocations("single application")
                .skipAttribution("single execution")
                .build();

    }
}

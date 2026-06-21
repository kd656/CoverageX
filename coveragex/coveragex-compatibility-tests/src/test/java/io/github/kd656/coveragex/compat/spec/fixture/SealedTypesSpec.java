package io.github.kd656.coveragex.compat.spec.fixture;

import io.github.kd656.coveragex.compat.contract.HitsContract;
import io.github.kd656.coveragex.compat.contract.PlanContract;
import io.github.kd656.coveragex.compat.spec.FixtureContractSpec;
import io.github.kd656.coveragex.compat.spec.FixtureContracts;

/**
 * Spec for {@code io.github.kd656.coveragex.fixtures.SealedTypes}.
 *
 * <p>Migrated from the legacy {@code SealedTypesContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class SealedTypesSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "io.github.kd656.coveragex.fixtures.SealedTypes";
    }

    @Override
    public FixtureContracts contracts() {
        return FixtureContracts.builder()
                .plan(PlanContract.builder().methodProbes(3).build())
                .hits(HitsContract.atLeastMethods(2, false, false))
                .skipArgs("args are records (Circle, Square); structured toString may shift across JDKs")
                .skipInvocations("each case hit once")
                .skipAttribution("requires constructing Circle/Square records reflectively in the plan; deferred until runner gains ctor-reflection support")
                .build();

    }
}

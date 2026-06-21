package io.github.kd656.coveragex.compat.spec.fixture;

import io.github.kd656.coveragex.compat.contract.HitsContract;
import io.github.kd656.coveragex.compat.contract.PlanContract;
import io.github.kd656.coveragex.compat.spec.FixtureContractSpec;
import io.github.kd656.coveragex.compat.spec.FixtureContracts;

/**
 * Spec for {@code io.github.kd656.coveragex.fixtures.PatternSealedSwitch}.
 *
 * <p>Migrated from the legacy {@code PatternSealedSwitchContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class PatternSealedSwitchSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "io.github.kd656.coveragex.fixtures.PatternSealedSwitch";
    }

    @Override
    public FixtureContracts contracts() {
        return FixtureContracts.builder()
                .plan(PlanContract.builder().methodProbes(3).build())
                .hits(HitsContract.atLeastMethods(2, false, false))
                .skipArgs("args are records (Dog, Cat); structured toString may shift across JDKs")
                .skipInvocations("each case hit once")
                .skipAttribution("requires reflectively constructing Dog/Cat records in the plan; deferred until runner gains ctor-reflection support")
                .build();

    }
}

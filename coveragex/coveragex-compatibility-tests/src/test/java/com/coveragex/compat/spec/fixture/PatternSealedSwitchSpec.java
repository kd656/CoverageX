package com.coveragex.compat.spec.fixture;

import com.coveragex.compat.contract.HitsContract;
import com.coveragex.compat.contract.PlanContract;
import com.coveragex.compat.spec.FixtureContractSpec;
import com.coveragex.compat.spec.FixtureContracts;

/**
 * Spec for {@code com.coveragex.fixtures.PatternSealedSwitch}.
 *
 * <p>Migrated from the legacy {@code PatternSealedSwitchContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class PatternSealedSwitchSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "com.coveragex.fixtures.PatternSealedSwitch";
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

package com.coveragex.compat.spec.fixture;

import com.coveragex.compat.contract.HitsContract;
import com.coveragex.compat.contract.PlanContract;
import com.coveragex.compat.spec.FixtureContractSpec;
import com.coveragex.compat.spec.FixtureContracts;

/**
 * Spec for {@code com.coveragex.fixtures.TextBlock}.
 *
 * <p>Migrated from the legacy {@code TextBlockContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class TextBlockSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "com.coveragex.fixtures.TextBlock";
    }

    @Override
    public FixtureContracts contracts() {
        return FixtureContracts.builder()
                .plan(PlanContract.builder().methodProbes(3).build())
                .hits(HitsContract.atLeastMethods(2, false, false))
                .skipArgs("banner() takes no arguments")
                .skipInvocations("single call")
                .skipAttribution("single execution; this fixture exists to verify text-block source-line tracking, not args/attribution")
                .build();

    }
}

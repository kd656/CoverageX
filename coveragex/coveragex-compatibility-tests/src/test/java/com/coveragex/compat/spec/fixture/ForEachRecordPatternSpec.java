package com.coveragex.compat.spec.fixture;

import com.coveragex.compat.contract.HitsContract;
import com.coveragex.compat.contract.PlanContract;
import com.coveragex.compat.spec.FixtureContractSpec;
import com.coveragex.compat.spec.FixtureContracts;

/**
 * Spec for {@code com.coveragex.fixtures.ForEachRecordPattern}.
 *
 * <p>Migrated from the legacy {@code ForEachRecordPatternContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class ForEachRecordPatternSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "com.coveragex.fixtures.ForEachRecordPattern";
    }

    @Override
    public FixtureContracts contracts() {
        return FixtureContracts.builder()
                .plan(PlanContract.builder().methodProbes(3).build())
                .hits(HitsContract.atLeastMethods(2, false, false))
                .skipArgs("List<Point> argument; collection of records")
                .skipInvocations("single outer call")
                .skipAttribution("single straight-line execution over the list")
                .build();

    }
}

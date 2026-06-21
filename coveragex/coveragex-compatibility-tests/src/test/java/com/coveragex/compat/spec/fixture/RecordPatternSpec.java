package com.coveragex.compat.spec.fixture;

import com.coveragex.compat.contract.HitsContract;
import com.coveragex.compat.contract.PlanContract;
import com.coveragex.compat.spec.FixtureContractSpec;
import com.coveragex.compat.spec.FixtureContracts;

/**
 * Spec for {@code com.coveragex.fixtures.RecordPattern}.
 *
 * <p>Migrated from the legacy {@code RecordPatternContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class RecordPatternSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "com.coveragex.fixtures.RecordPattern";
    }

    @Override
    public FixtureContracts contracts() {
        return FixtureContracts.builder()
                .plan(PlanContract.builder().methodProbes(3).build())
                .hits(HitsContract.atLeastMethods(2, false, false))
                .skipArgs("args are records (Point) with structured toString; pinning brittle to JDK record auto-string format")
                .skipInvocations("each call once")
                .skipAttribution("the match-path step requires reflectively constructing a Point; deferred until runner gains ctor-reflection support")
                .build();

    }
}

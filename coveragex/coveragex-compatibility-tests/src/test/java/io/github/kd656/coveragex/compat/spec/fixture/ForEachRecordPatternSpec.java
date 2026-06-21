package io.github.kd656.coveragex.compat.spec.fixture;

import io.github.kd656.coveragex.compat.contract.HitsContract;
import io.github.kd656.coveragex.compat.contract.PlanContract;
import io.github.kd656.coveragex.compat.spec.FixtureContractSpec;
import io.github.kd656.coveragex.compat.spec.FixtureContracts;

/**
 * Spec for {@code io.github.kd656.coveragex.fixtures.ForEachRecordPattern}.
 *
 * <p>Migrated from the legacy {@code ForEachRecordPatternContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class ForEachRecordPatternSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "io.github.kd656.coveragex.fixtures.ForEachRecordPattern";
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

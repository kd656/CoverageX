package io.github.kd656.coveragex.compat.spec.fixture;

import io.github.kd656.coveragex.api.data.ProbeMetadata.BranchDirection;
import io.github.kd656.coveragex.compat.contract.HitsContract;
import io.github.kd656.coveragex.compat.contract.PlanContract;
import io.github.kd656.coveragex.compat.spec.FixtureContractSpec;
import io.github.kd656.coveragex.compat.spec.FixtureContracts;
import java.util.List;

/**
 * Spec for {@code io.github.kd656.coveragex.fixtures.ForEachIterable}.
 *
 * <p>Migrated from the legacy {@code ForEachIterableContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class ForEachIterableSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "io.github.kd656.coveragex.fixtures.ForEachIterable";
    }

    @Override
    public FixtureContracts contracts() {
        return FixtureContracts.builder()
                .plan(PlanContract.builder()
                .methodProbes(3)
                .anyLineBranch(BranchDirection.TRUE)
                .anyLineBranch(BranchDirection.FALSE)
                .build())
                .hits(new HitsContract(2, 0, 0, true, true, List.of(), List.of()))
                .skipArgs("List.of(...) toString format may shift across JDKs")
                .skipInvocations("single outer call")
                .skipAttribution("single execution")
                .build();

    }
}

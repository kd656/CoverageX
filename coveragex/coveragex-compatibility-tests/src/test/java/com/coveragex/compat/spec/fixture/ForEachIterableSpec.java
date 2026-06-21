package com.coveragex.compat.spec.fixture;

import com.coveragex.api.data.ProbeMetadata.BranchDirection;
import com.coveragex.compat.contract.HitsContract;
import com.coveragex.compat.contract.PlanContract;
import com.coveragex.compat.spec.FixtureContractSpec;
import com.coveragex.compat.spec.FixtureContracts;
import java.util.List;

/**
 * Spec for {@code com.coveragex.fixtures.ForEachIterable}.
 *
 * <p>Migrated from the legacy {@code ForEachIterableContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class ForEachIterableSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "com.coveragex.fixtures.ForEachIterable";
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

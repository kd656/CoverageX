package com.coveragex.compat.spec.fixture;

import com.coveragex.api.data.ProbeMetadata.BranchDirection;
import com.coveragex.compat.contract.HitsContract;
import com.coveragex.compat.contract.HitsContract.BranchHitExpectation;
import com.coveragex.compat.contract.PlanContract;
import com.coveragex.compat.spec.FixtureContractSpec;
import com.coveragex.compat.spec.FixtureContracts;
import java.util.List;
import com.coveragex.compat.contract.ArgsContract;

/**
 * Spec for {@code com.coveragex.fixtures.WhileDo}.
 *
 * <p>Migrated from the legacy {@code WhileDoContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class WhileDoSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "com.coveragex.fixtures.WhileDo";
    }

    @Override
    public FixtureContracts contracts() {
        return FixtureContracts.builder()
                .plan(PlanContract.builder()
                .methodProbes(3)
                .branch(6, BranchDirection.TRUE)
                .branch(6, BranchDirection.FALSE)
                .build())
                .hits(new HitsContract(2, 0, 0, true, true,
                        List.of(
                                BranchHitExpectation.atLeast(6, BranchDirection.TRUE,  2),
                                BranchHitExpectation.atLeast(6, BranchDirection.FALSE, 1)),
                        List.of()))
                .args(ArgsContract.builder()
                        .method("countDown", List.of(List.of("2")))
                        .build())
                .skipInvocations("single outer call")
                .skipAttribution("single execution")
                .build();

    }
}

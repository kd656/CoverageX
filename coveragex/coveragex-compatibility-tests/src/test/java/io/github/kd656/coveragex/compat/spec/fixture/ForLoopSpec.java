package io.github.kd656.coveragex.compat.spec.fixture;

import io.github.kd656.coveragex.api.data.ProbeMetadata.BranchDirection;
import io.github.kd656.coveragex.compat.contract.HitsContract;
import io.github.kd656.coveragex.compat.contract.HitsContract.BranchHitExpectation;
import io.github.kd656.coveragex.compat.contract.PlanContract;
import io.github.kd656.coveragex.compat.spec.FixtureContractSpec;
import io.github.kd656.coveragex.compat.spec.FixtureContracts;
import java.util.List;
import io.github.kd656.coveragex.compat.contract.ArgsContract;

/**
 * Spec for {@code io.github.kd656.coveragex.fixtures.ForLoop}.
 *
 * <p>Migrated from the legacy {@code ForLoopContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class ForLoopSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "io.github.kd656.coveragex.fixtures.ForLoop";
    }

    @Override
    public FixtureContracts contracts() {
        return FixtureContracts.builder()
                .plan(PlanContract.builder()
                .methodProbes(3)
                .branch(7, BranchDirection.TRUE)
                .branch(7, BranchDirection.FALSE)
                .build())
                .hits(new HitsContract(2, 0, 0, true, true,
                        List.of(
                                BranchHitExpectation.atLeast(7, BranchDirection.FALSE, 3),
                                BranchHitExpectation.atLeast(7, BranchDirection.TRUE,  1)),
                        List.of()))
                .args(ArgsContract.builder()
                        .method("sumTo", List.of(List.of("3")))
                        .build())
                .skipInvocations("single outer call; loop body is straight-line code, not a callable")
                .skipAttribution("single call, no useful split")
                .build();

    }
}

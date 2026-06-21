package io.github.kd656.coveragex.compat.spec.fixture;

import io.github.kd656.coveragex.api.data.ProbeMetadata.BranchDirection;
import io.github.kd656.coveragex.compat.contract.HitsContract;
import io.github.kd656.coveragex.compat.contract.PlanContract;
import io.github.kd656.coveragex.compat.spec.FixtureContractSpec;
import io.github.kd656.coveragex.compat.spec.FixtureContracts;
import java.util.List;
import io.github.kd656.coveragex.compat.contract.ArgsContract;

/**
 * Spec for {@code io.github.kd656.coveragex.fixtures.DoWhile}.
 *
 * <p>Migrated from the legacy {@code DoWhileContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class DoWhileSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "io.github.kd656.coveragex.fixtures.DoWhile";
    }

    @Override
    public FixtureContracts contracts() {
        return FixtureContracts.builder()
                .plan(PlanContract.builder()
                .methodProbes(3)
                .branch(8, BranchDirection.TRUE)
                .branch(8, BranchDirection.FALSE)
                .build())
                .hits(new HitsContract(2, 0, 0, false, true, List.of(), List.of()))
                .args(ArgsContract.builder()
                        .method("countDown", List.of(List.of("1")))
                        .build())
                .skipInvocations("single outer call")
                .skipAttribution("single execution")
                .build();

    }
}

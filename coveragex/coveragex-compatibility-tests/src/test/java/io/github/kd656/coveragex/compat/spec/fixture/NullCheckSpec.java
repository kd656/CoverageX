package io.github.kd656.coveragex.compat.spec.fixture;

import io.github.kd656.coveragex.api.data.ProbeMetadata.BranchDirection;
import io.github.kd656.coveragex.compat.contract.HitsContract;
import io.github.kd656.coveragex.compat.contract.HitsContract.BranchHitExpectation;
import io.github.kd656.coveragex.compat.contract.PlanContract;
import io.github.kd656.coveragex.compat.spec.FixtureContractSpec;
import io.github.kd656.coveragex.compat.spec.FixtureContracts;
import java.util.List;
import io.github.kd656.coveragex.compat.contract.ArgsContract;
import io.github.kd656.coveragex.compat.contract.TestAttributionContract;
import io.github.kd656.coveragex.compat.spec.InvocationStep;
import java.util.Arrays;

/**
 * Spec for {@code io.github.kd656.coveragex.fixtures.NullCheck}.
 *
 * <p>Migrated from the legacy {@code NullCheckContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class NullCheckSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "io.github.kd656.coveragex.fixtures.NullCheck";
    }

    @Override
    public FixtureContracts contracts() {
        return FixtureContracts.builder()
                .plan(PlanContract.builder()
                .methodProbes(3)
                .branch(6, BranchDirection.TRUE)
                .branch(6, BranchDirection.FALSE)
                .build())
                .hits(new HitsContract(
                2, 0, 0, true, true,
                List.of(
                        BranchHitExpectation.atLeastOnce(6, BranchDirection.TRUE),
                        BranchHitExpectation.atLeastOnce(6, BranchDirection.FALSE)),
                List.of()))
                .args(ArgsContract.builder()
                        .method("orDefault", List.of(Arrays.<String>asList((String) null), List.of("hello")))
                        .build())
                .skipInvocations("each branch hit once")
                .attribution(TestAttributionContract.builder()
                        .method("orDefault", "NullCheck#null-path", "NullCheck#happy-path")
                        .build())
                .build();

    }

    @Override
    public List<InvocationStep> invocationPlan() {
        return List.of(
                InvocationStep.of("orDefault", Arrays.asList((Object) null), "NullCheck#null-path"),
                InvocationStep.of("orDefault", List.of("hello"), "NullCheck#happy-path"));
    }
}

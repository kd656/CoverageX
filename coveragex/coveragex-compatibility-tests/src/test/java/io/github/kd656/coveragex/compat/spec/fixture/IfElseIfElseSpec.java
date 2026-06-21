package io.github.kd656.coveragex.compat.spec.fixture;

import io.github.kd656.coveragex.api.data.ProbeMetadata.BranchDirection;
import io.github.kd656.coveragex.compat.contract.HitsContract;
import io.github.kd656.coveragex.compat.contract.PlanContract;
import io.github.kd656.coveragex.compat.spec.FixtureContractSpec;
import io.github.kd656.coveragex.compat.spec.FixtureContracts;
import java.util.List;
import io.github.kd656.coveragex.compat.contract.ArgsContract;
import io.github.kd656.coveragex.compat.contract.TestAttributionContract;
import io.github.kd656.coveragex.compat.spec.InvocationStep;

/**
 * Spec for {@code io.github.kd656.coveragex.fixtures.IfElseIfElse}.
 *
 * <p>Migrated from the legacy {@code IfElseIfElseContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class IfElseIfElseSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "io.github.kd656.coveragex.fixtures.IfElseIfElse";
    }

    @Override
    public FixtureContracts contracts() {
        return FixtureContracts.builder()
                .plan(PlanContract.builder()
                .methodProbes(3)
                .branch(6, BranchDirection.TRUE)
                .branch(6, BranchDirection.FALSE)
                .branch(8, BranchDirection.TRUE)
                .branch(8, BranchDirection.FALSE)
                .build())
                .hits(new HitsContract(
                2, 0, 0, true, true,
                List.of(),
                List.of()))
                .args(ArgsContract.builder()
                        .method("sign", List.of(List.of("1"), List.of("-1"), List.of("0")))
                        .build())
                .skipInvocations("each branch hit once")
                .attribution(TestAttributionContract.builder()
                        .method("sign", "IfElseIfElse#pos", "IfElseIfElse#neg", "IfElseIfElse#zero")
                        .build())
                .build();

    }

    @Override
    public List<InvocationStep> invocationPlan() {
        return List.of(
                InvocationStep.of("sign", List.of(1), "IfElseIfElse#pos"),
                InvocationStep.of("sign", List.of(-1), "IfElseIfElse#neg"),
                InvocationStep.of("sign", List.of(0), "IfElseIfElse#zero"));
    }
}

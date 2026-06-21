package io.github.kd656.coveragex.compat.spec.fixture;

import io.github.kd656.coveragex.api.data.ProbeMetadata.BranchDirection;
import io.github.kd656.coveragex.compat.contract.ArgsContract;
import io.github.kd656.coveragex.compat.contract.HitsContract;
import io.github.kd656.coveragex.compat.contract.HitsContract.BranchHitExpectation;
import io.github.kd656.coveragex.compat.contract.InvocationContract;
import io.github.kd656.coveragex.compat.contract.PlanContract;
import io.github.kd656.coveragex.compat.spec.FixtureContractSpec;
import io.github.kd656.coveragex.compat.spec.FixtureContracts;
import java.util.List;
import io.github.kd656.coveragex.compat.contract.TestAttributionContract;
import io.github.kd656.coveragex.compat.spec.InvocationStep;

/**
 * Spec for {@code io.github.kd656.coveragex.fixtures.Recursive}.
 *
 * <p>Migrated from the legacy {@code RecursiveContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class RecursiveSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "io.github.kd656.coveragex.fixtures.Recursive";
    }

    @Override
    public FixtureContracts contracts() {
        return FixtureContracts.builder()
                .plan(PlanContract.builder()
                .methodProbes(3)                                // <init> + factorial + execute
                .branch(6, BranchDirection.TRUE)
                .branch(6, BranchDirection.FALSE)
                .returnsOnLines(3, 7, 9, 14)
                .build())
                .hits(new HitsContract(
                /* minMethodHits             */ 2,
                /* minReturnHits             */ 2,
                /* minThrowHits              */ 0,
                /* requireTrueBranchHit      */ true,
                /* requireFalseBranchHit     */ true,
                List.of(
                        BranchHitExpectation.atLeastOnce(6, BranchDirection.TRUE),
                        BranchHitExpectation.atLeastOnce(6, BranchDirection.FALSE)),
                List.of()))
                .args(ArgsContract.builder()
                        .method("factorial", List.of(List.of("5"), List.of("4"), List.of("3"), List.of("2"), List.of("1")))
                        .build())
                .invocations(InvocationContract.builder()
                        .method("factorial", 5)
                        .method("execute", 1)
                        .build())
                .attribution(TestAttributionContract.builder()
                        .method("factorial", "Recursive#factorial")
                        .build())
                .build();

    }

    @Override
    public List<InvocationStep> invocationPlan() {
        return List.of(
                InvocationStep.of("factorial", List.of(5), "Recursive#factorial"));
    }
}

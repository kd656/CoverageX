package com.coveragex.compat.spec.fixture;

import com.coveragex.api.data.ProbeMetadata.BranchDirection;
import com.coveragex.compat.contract.ArgsContract;
import com.coveragex.compat.contract.HitsContract;
import com.coveragex.compat.contract.HitsContract.BranchHitExpectation;
import com.coveragex.compat.contract.InvocationContract;
import com.coveragex.compat.contract.PlanContract;
import com.coveragex.compat.spec.FixtureContractSpec;
import com.coveragex.compat.spec.FixtureContracts;
import java.util.List;
import com.coveragex.compat.contract.TestAttributionContract;
import com.coveragex.compat.spec.InvocationStep;

/**
 * Spec for {@code com.coveragex.fixtures.Recursive}.
 *
 * <p>Migrated from the legacy {@code RecursiveContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class RecursiveSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "com.coveragex.fixtures.Recursive";
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

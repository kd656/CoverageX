package com.coveragex.compat.spec.fixture;

import com.coveragex.compat.contract.HitsContract;
import com.coveragex.compat.contract.PlanContract;
import com.coveragex.compat.spec.FixtureContractSpec;
import com.coveragex.compat.spec.FixtureContracts;
import java.util.List;
import com.coveragex.compat.contract.ArgsContract;
import com.coveragex.compat.contract.TestAttributionContract;
import com.coveragex.compat.spec.InvocationStep;

/**
 * Spec for {@code com.coveragex.fixtures.TryWithResources}.
 *
 * <p>Migrated from the legacy {@code TryWithResourcesContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class TryWithResourcesSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "com.coveragex.fixtures.TryWithResources";
    }

    @Override
    public FixtureContracts contracts() {
        return FixtureContracts.builder()
                .plan(PlanContract.builder()
                .methodProbes(3)                                // <init> + read + execute
                .returnsOnLines(3, 8, 10, 17)
                .throwsOnLines(6, 7)
                .build())
                .hits(new HitsContract(
                /* minMethodHits             */ 2,
                /* minReturnHits             */ 2,
                /* minThrowHits              */ 1,
                /* requireTrueBranchHit      */ true,
                /* requireFalseBranchHit     */ true,
                List.of(),                                       // no line-pinned branch claims (TWR shape varies)
                List.of()))
                .args(ArgsContract.builder()
                        .method("read", List.of(List.of("false"), List.of("true")))
                        .build())
                .skipInvocations("each call once")
                .attribution(TestAttributionContract.builder()
                        .method("read", "TWR#normal-exit", "TWR#throw-catch")
                        .build())
                .build();

    }

    @Override
    public List<InvocationStep> invocationPlan() {
        return List.of(
                InvocationStep.of("read", List.of(false), "TWR#normal-exit"),
                InvocationStep.of("read", List.of(true), "TWR#throw-catch"));
    }
}

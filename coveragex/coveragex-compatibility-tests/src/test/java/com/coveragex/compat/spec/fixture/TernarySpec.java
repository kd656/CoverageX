package com.coveragex.compat.spec.fixture;

import com.coveragex.api.data.ProbeMetadata.BranchDirection;
import com.coveragex.compat.contract.HitsContract;
import com.coveragex.compat.contract.PlanContract;
import com.coveragex.compat.spec.FixtureContractSpec;
import com.coveragex.compat.spec.FixtureContracts;
import java.util.List;
import com.coveragex.compat.contract.ArgsContract;
import com.coveragex.compat.contract.TestAttributionContract;
import com.coveragex.compat.spec.InvocationStep;

/**
 * Spec for {@code com.coveragex.fixtures.Ternary}.
 *
 * <p>Migrated from the legacy {@code TernaryContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class TernarySpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "com.coveragex.fixtures.Ternary";
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
                List.of(), List.of()))
                .args(ArgsContract.builder()
                        .method("abs", List.of(List.of("3"), List.of("-3")))
                        .build())
                .skipInvocations("each branch hit once")
                .attribution(TestAttributionContract.builder()
                        .method("abs", "Ternary#pos", "Ternary#neg")
                        .build())
                .build();

    }

    @Override
    public List<InvocationStep> invocationPlan() {
        return List.of(
                InvocationStep.of("abs", List.of(3), "Ternary#pos"),
                InvocationStep.of("abs", List.of(-3), "Ternary#neg"));
    }
}

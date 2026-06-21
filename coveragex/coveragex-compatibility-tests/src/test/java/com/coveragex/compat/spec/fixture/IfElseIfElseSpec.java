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
 * Spec for {@code com.coveragex.fixtures.IfElseIfElse}.
 *
 * <p>Migrated from the legacy {@code IfElseIfElseContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class IfElseIfElseSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "com.coveragex.fixtures.IfElseIfElse";
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

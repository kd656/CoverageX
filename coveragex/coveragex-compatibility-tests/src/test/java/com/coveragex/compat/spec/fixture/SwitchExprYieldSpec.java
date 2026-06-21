package com.coveragex.compat.spec.fixture;

import com.coveragex.compat.contract.HitsContract;
import com.coveragex.compat.contract.PlanContract;
import com.coveragex.compat.spec.FixtureContractSpec;
import com.coveragex.compat.spec.FixtureContracts;
import com.coveragex.compat.contract.ArgsContract;
import com.coveragex.compat.contract.TestAttributionContract;
import com.coveragex.compat.spec.InvocationStep;
import java.util.List;

/**
 * Spec for {@code com.coveragex.fixtures.SwitchExprYield}.
 *
 * <p>Migrated from the legacy {@code SwitchExprYieldContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class SwitchExprYieldSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "com.coveragex.fixtures.SwitchExprYield";
    }

    @Override
    public FixtureContracts contracts() {
        return FixtureContracts.builder()
                .plan(PlanContract.builder().methodProbes(3).build())
                .hits(HitsContract.atLeastMethods(2, false, false))
                .args(ArgsContract.builder()
                        .method("classify", List.of(List.of("1"), List.of("2"), List.of("9")))
                        .build())
                .skipInvocations("each case hit once")
                .attribution(TestAttributionContract.builder()
                        .method("classify", "SwitchExprYield#one", "SwitchExprYield#two", "SwitchExprYield#default")
                        .build())
                .build();

    }

    @Override
    public List<InvocationStep> invocationPlan() {
        return List.of(
                InvocationStep.of("classify", List.of(1), "SwitchExprYield#one"),
                InvocationStep.of("classify", List.of(2), "SwitchExprYield#two"),
                InvocationStep.of("classify", List.of(9), "SwitchExprYield#default"));
    }
}

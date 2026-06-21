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
 * Spec for {@code com.coveragex.fixtures.SwitchExprReturn}.
 *
 * <p>Migrated from the legacy {@code SwitchExprReturnContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class SwitchExprReturnSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "com.coveragex.fixtures.SwitchExprReturn";
    }

    @Override
    public FixtureContracts contracts() {
        return FixtureContracts.builder()
                .plan(PlanContract.builder().methodProbes(3).build())
                .hits(HitsContract.atLeastMethods(2, false, false))
                .args(ArgsContract.builder()
                        .method("name", List.of(List.of("1"), List.of("2"), List.of("99")))
                        .build())
                .skipInvocations("each case hit once")
                .attribution(TestAttributionContract.builder()
                        .method("name", "SwitchExprReturn#one", "SwitchExprReturn#two", "SwitchExprReturn#default")
                        .build())
                .build();

    }

    @Override
    public List<InvocationStep> invocationPlan() {
        return List.of(
                InvocationStep.of("name", List.of(1), "SwitchExprReturn#one"),
                InvocationStep.of("name", List.of(2), "SwitchExprReturn#two"),
                InvocationStep.of("name", List.of(99), "SwitchExprReturn#default"));
    }
}

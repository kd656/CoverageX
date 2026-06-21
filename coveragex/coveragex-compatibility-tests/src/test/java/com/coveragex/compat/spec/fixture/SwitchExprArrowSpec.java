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
 * Spec for {@code com.coveragex.fixtures.SwitchExprArrow}.
 *
 * <p>Migrated from the legacy {@code SwitchExprArrowContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class SwitchExprArrowSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "com.coveragex.fixtures.SwitchExprArrow";
    }

    @Override
    public FixtureContracts contracts() {
        return FixtureContracts.builder()
                .plan(PlanContract.builder().methodProbes(3).build())
                .hits(HitsContract.atLeastMethods(2, false, false))
                .args(ArgsContract.builder()
                        .method("label", List.of(List.of("1"), List.of("2"), List.of("3"), List.of("99")))
                        .build())
                .skipInvocations("each case hit once")
                .attribution(TestAttributionContract.builder()
                        .method("label", "SwitchExprArrow#one", "SwitchExprArrow#two", "SwitchExprArrow#three", "SwitchExprArrow#default")
                        .build())
                .build();

    }

    @Override
    public List<InvocationStep> invocationPlan() {
        return List.of(
                InvocationStep.of("label", List.of(1), "SwitchExprArrow#one"),
                InvocationStep.of("label", List.of(2), "SwitchExprArrow#two"),
                InvocationStep.of("label", List.of(3), "SwitchExprArrow#three"),
                InvocationStep.of("label", List.of(99), "SwitchExprArrow#default"));
    }
}

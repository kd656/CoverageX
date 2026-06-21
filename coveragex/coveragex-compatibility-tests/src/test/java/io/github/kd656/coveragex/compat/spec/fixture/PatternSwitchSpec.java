package io.github.kd656.coveragex.compat.spec.fixture;

import io.github.kd656.coveragex.compat.contract.HitsContract;
import io.github.kd656.coveragex.compat.contract.PlanContract;
import io.github.kd656.coveragex.compat.spec.FixtureContractSpec;
import io.github.kd656.coveragex.compat.spec.FixtureContracts;
import io.github.kd656.coveragex.compat.contract.ArgsContract;
import io.github.kd656.coveragex.compat.contract.TestAttributionContract;
import io.github.kd656.coveragex.compat.spec.InvocationStep;
import java.util.List;

/**
 * Spec for {@code io.github.kd656.coveragex.fixtures.PatternSwitch}.
 *
 * <p>Migrated from the legacy {@code PatternSwitchContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class PatternSwitchSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "io.github.kd656.coveragex.fixtures.PatternSwitch";
    }

    @Override
    public FixtureContracts contracts() {
        return FixtureContracts.builder()
                .plan(PlanContract.builder().methodProbes(3).build())
                .hits(HitsContract.atLeastMethods(2, false, false))
                .args(ArgsContract.builder()
                        .method("describe", List.of(List.of("42"), List.of("hi"), List.of("3.14")))
                        .build())
                .skipInvocations("each case hit once")
                .attribution(TestAttributionContract.builder()
                        .method("describe", "PatternSwitch#int", "PatternSwitch#str", "PatternSwitch#default")
                        .build())
                .build();

    }

    @Override
    public List<InvocationStep> invocationPlan() {
        return List.of(
                InvocationStep.of("describe", List.of(42), "PatternSwitch#int"),
                InvocationStep.of("describe", List.of("hi"), "PatternSwitch#str"),
                InvocationStep.of("describe", List.of(3.14), "PatternSwitch#default"));
    }
}

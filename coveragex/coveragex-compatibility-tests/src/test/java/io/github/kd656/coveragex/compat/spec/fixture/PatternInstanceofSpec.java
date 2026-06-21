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
 * Spec for {@code io.github.kd656.coveragex.fixtures.PatternInstanceof}.
 *
 * <p>Migrated from the legacy {@code PatternInstanceofContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class PatternInstanceofSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "io.github.kd656.coveragex.fixtures.PatternInstanceof";
    }

    @Override
    public FixtureContracts contracts() {
        return FixtureContracts.builder()
                .plan(PlanContract.builder().methodProbes(3).build())
                .hits(HitsContract.atLeastMethods(2, false, false))
                .args(ArgsContract.builder()
                        .method("lengthOf", List.of(List.of("hello"), List.of("123")))
                        .build())
                .skipInvocations("each call once")
                .attribution(TestAttributionContract.builder()
                        .method("lengthOf", "PatternInstanceof#match", "PatternInstanceof#no-match")
                        .build())
                .build();

    }

    @Override
    public List<InvocationStep> invocationPlan() {
        return List.of(
                InvocationStep.of("lengthOf", List.of("hello"), "PatternInstanceof#match"),
                InvocationStep.of("lengthOf", List.of(123), "PatternInstanceof#no-match"));
    }
}

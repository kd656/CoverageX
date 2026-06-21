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
 * Spec for {@code com.coveragex.fixtures.PatternInstanceof}.
 *
 * <p>Migrated from the legacy {@code PatternInstanceofContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class PatternInstanceofSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "com.coveragex.fixtures.PatternInstanceof";
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

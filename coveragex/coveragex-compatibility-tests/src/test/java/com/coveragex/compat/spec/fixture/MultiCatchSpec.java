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
 * Spec for {@code com.coveragex.fixtures.MultiCatch}.
 *
 * <p>Migrated from the legacy {@code MultiCatchContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class MultiCatchSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "com.coveragex.fixtures.MultiCatch";
    }

    @Override
    public FixtureContracts contracts() {
        return FixtureContracts.builder()
                .plan(PlanContract.builder().methodProbes(3).build())
                .hits(new HitsContract(2, 3, 2, false, false, List.of(), List.of()))
                .args(ArgsContract.builder()
                        .method("classify", List.of(List.of("0"), List.of("1"), List.of("2")))
                        .build())
                .skipInvocations("each call once")
                .attribution(TestAttributionContract.builder()
                        .method("classify", "MultiCatch#no-throw", "MultiCatch#ioe", "MultiCatch#iae")
                        .build())
                .build();

    }

    @Override
    public List<InvocationStep> invocationPlan() {
        return List.of(
                InvocationStep.of("classify", List.of(0), "MultiCatch#no-throw"),
                InvocationStep.of("classify", List.of(1), "MultiCatch#ioe"),
                InvocationStep.of("classify", List.of(2), "MultiCatch#iae"));
    }
}

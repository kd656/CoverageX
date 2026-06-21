package io.github.kd656.coveragex.compat.spec.fixture;

import io.github.kd656.coveragex.compat.contract.HitsContract;
import io.github.kd656.coveragex.compat.contract.PlanContract;
import io.github.kd656.coveragex.compat.spec.FixtureContractSpec;
import io.github.kd656.coveragex.compat.spec.FixtureContracts;
import java.util.List;
import io.github.kd656.coveragex.compat.contract.ArgsContract;
import io.github.kd656.coveragex.compat.contract.TestAttributionContract;
import io.github.kd656.coveragex.compat.spec.InvocationStep;

/**
 * Spec for {@code io.github.kd656.coveragex.fixtures.TryCatchFinally}.
 *
 * <p>Migrated from the legacy {@code TryCatchFinallyContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class TryCatchFinallySpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "io.github.kd656.coveragex.fixtures.TryCatchFinally";
    }

    @Override
    public FixtureContracts contracts() {
        return FixtureContracts.builder()
                .plan(PlanContract.builder().methodProbes(3).build())
                .hits(new HitsContract(2, 2, 0, false, false, List.of(), List.of()))
                .args(ArgsContract.builder()
                        .method("parse", List.of(List.of("42"), List.of("nope")))
                        .build())
                .skipInvocations("each call once")
                .attribution(TestAttributionContract.builder()
                        .method("parse", "TryCatchFinally#happy", "TryCatchFinally#throw")
                        .build())
                .build();

    }

    @Override
    public List<InvocationStep> invocationPlan() {
        return List.of(
                InvocationStep.of("parse", List.of("42"), "TryCatchFinally#happy"),
                InvocationStep.of("parse", List.of("nope"), "TryCatchFinally#throw"));
    }
}

package io.github.kd656.coveragex.compat.spec.fixture;

import io.github.kd656.coveragex.compat.contract.HitsContract;
import io.github.kd656.coveragex.compat.contract.PlanContract;
import io.github.kd656.coveragex.compat.spec.FixtureContractSpec;
import io.github.kd656.coveragex.compat.spec.FixtureContracts;

/**
 * Spec for {@code io.github.kd656.coveragex.fixtures.InnerClass}.
 *
 * <p>Migrated from the legacy {@code InnerClassContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class InnerClassSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "io.github.kd656.coveragex.fixtures.InnerClass";
    }

    @Override
    public FixtureContracts contracts() {
        return FixtureContracts.builder()
                .plan(PlanContract.builder().methodProbes(2).build())
                .hits(HitsContract.atLeastMethods(1, false, false))
                .skipArgs("doubled() lives on nested class Inner; runner currently inspects only the outer class's methodHits")
                .skipInvocations("single call")
                .skipAttribution("single execution")
                .build();

    }
}

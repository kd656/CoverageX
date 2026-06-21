package io.github.kd656.coveragex.compat.spec.fixture;

import io.github.kd656.coveragex.compat.contract.HitsContract;
import io.github.kd656.coveragex.compat.contract.PlanContract;
import io.github.kd656.coveragex.compat.spec.FixtureContractSpec;
import io.github.kd656.coveragex.compat.spec.FixtureContracts;

/**
 * Spec for {@code io.github.kd656.coveragex.fixtures.Lambda}.
 *
 * <p>Migrated from the legacy {@code LambdaContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class LambdaSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "io.github.kd656.coveragex.fixtures.Lambda";
    }

    @Override
    public FixtureContracts contracts() {
        return FixtureContracts.builder()
                .plan(PlanContract.builder()
                .methodProbes(3)   // <init> + execute + synthetic lambda$execute$0
                .build())
                .hits(HitsContract.atLeastMethods(2, false, false))
                .skipArgs("lambda body is a synthetic method with no user-meaningful args")
                .skipInvocations("single lambda invocation")
                .skipAttribution("single execution")
                .build();

    }
}

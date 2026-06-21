package io.github.kd656.coveragex.compat.spec.fixture;

import io.github.kd656.coveragex.compat.contract.HitsContract;
import io.github.kd656.coveragex.compat.contract.PlanContract;
import io.github.kd656.coveragex.compat.spec.FixtureContractSpec;
import io.github.kd656.coveragex.compat.spec.FixtureContracts;

/**
 * Spec for {@code io.github.kd656.coveragex.fixtures.TwrTwoResources}.
 *
 * <p>Migrated from the legacy {@code TwrTwoResourcesContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class TwrTwoResourcesSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "io.github.kd656.coveragex.fixtures.TwrTwoResources";
    }

    @Override
    public FixtureContracts contracts() {
        return FixtureContracts.builder()
                .plan(PlanContract.builder().methodProbes(3).build())
                .hits(HitsContract.atLeastMethods(2, false, false))
                .skipArgs("doWork() takes no arguments")
                .skipInvocations("single call")
                .skipAttribution("single execution")
                .build();

    }
}

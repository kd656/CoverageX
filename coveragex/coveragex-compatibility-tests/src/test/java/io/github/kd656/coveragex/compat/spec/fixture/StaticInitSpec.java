package io.github.kd656.coveragex.compat.spec.fixture;

import io.github.kd656.coveragex.compat.contract.HitsContract;
import io.github.kd656.coveragex.compat.contract.PlanContract;
import io.github.kd656.coveragex.compat.spec.FixtureContractSpec;
import io.github.kd656.coveragex.compat.spec.FixtureContracts;

/**
 * Spec for {@code io.github.kd656.coveragex.fixtures.StaticInit}.
 *
 * <p>Migrated from the legacy {@code StaticInitContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class StaticInitSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "io.github.kd656.coveragex.fixtures.StaticInit";
    }

    @Override
    public FixtureContracts contracts() {
        return FixtureContracts.builder()
                .plan(PlanContract.builder().methodProbes(4).build())
                .hits(HitsContract.atLeastMethods(3, false, false))
                .skipArgs("<clinit> and get() both no-arg")
                .skipInvocations("class-load only")
                .skipAttribution("<clinit> runs at class-load before any test scope")
                .build();

    }
}

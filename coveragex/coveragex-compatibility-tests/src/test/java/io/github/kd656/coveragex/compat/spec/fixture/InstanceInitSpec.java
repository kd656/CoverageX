package io.github.kd656.coveragex.compat.spec.fixture;

import io.github.kd656.coveragex.compat.contract.HitsContract;
import io.github.kd656.coveragex.compat.contract.PlanContract;
import io.github.kd656.coveragex.compat.spec.FixtureContractSpec;
import io.github.kd656.coveragex.compat.spec.FixtureContracts;
import io.github.kd656.coveragex.compat.contract.ArgsContract;
import java.util.List;

/**
 * Spec for {@code io.github.kd656.coveragex.fixtures.InstanceInit}.
 *
 * <p>Migrated from the legacy {@code InstanceInitContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class InstanceInitSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "io.github.kd656.coveragex.fixtures.InstanceInit";
    }

    @Override
    public FixtureContracts contracts() {
        return FixtureContracts.builder()
                .plan(PlanContract.builder().methodProbes(4).build())
                .hits(HitsContract.atLeastMethods(3, false, false))
                .args(ArgsContract.builder()
                        .method("<init>", List.of(List.<String>of(), List.of("7")))
                        .build())
                .skipInvocations("each ctor called once")
                .skipAttribution("both ctors fire from one execute; no per-test split")
                .build();

    }
}

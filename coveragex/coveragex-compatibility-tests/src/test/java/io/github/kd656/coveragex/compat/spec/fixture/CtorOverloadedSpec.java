package io.github.kd656.coveragex.compat.spec.fixture;

import io.github.kd656.coveragex.compat.contract.HitsContract;
import io.github.kd656.coveragex.compat.contract.PlanContract;
import io.github.kd656.coveragex.compat.spec.FixtureContractSpec;
import io.github.kd656.coveragex.compat.spec.FixtureContracts;
import io.github.kd656.coveragex.compat.contract.ArgsContract;
import io.github.kd656.coveragex.compat.contract.InvocationContract;
import java.util.List;

/**
 * Spec for {@code io.github.kd656.coveragex.fixtures.CtorOverloaded}.
 *
 * <p>Migrated from the legacy {@code CtorOverloadedContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class CtorOverloadedSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "io.github.kd656.coveragex.fixtures.CtorOverloaded";
    }

    @Override
    public FixtureContracts contracts() {
        return FixtureContracts.builder()
                .plan(PlanContract.builder().methodProbes(4).build())
                .hits(HitsContract.atLeastMethods(3, false, false))
                .args(ArgsContract.builder()
                        .method("<init>", List.of(List.<String>of(), List.of("0"), List.of("7")))
                        .build())
                .invocations(InvocationContract.builder()
                        .method("<init>", 3)
                        .build())
                .skipAttribution("ctors fire together in execute; splitting cosmetic")
                .build();

    }
}

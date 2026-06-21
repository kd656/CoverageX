package io.github.kd656.coveragex.compat.spec.fixture;

import io.github.kd656.coveragex.compat.contract.HitsContract;
import io.github.kd656.coveragex.compat.contract.PlanContract;
import io.github.kd656.coveragex.compat.spec.FixtureContractSpec;
import io.github.kd656.coveragex.compat.spec.FixtureContracts;
import io.github.kd656.coveragex.compat.contract.ArgsContract;
import java.util.List;

/**
 * Spec for {@code io.github.kd656.coveragex.fixtures.StringConcat}.
 *
 * <p>Migrated from the legacy {@code StringConcatContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class StringConcatSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "io.github.kd656.coveragex.fixtures.StringConcat";
    }

    @Override
    public FixtureContracts contracts() {
        return FixtureContracts.builder()
                .plan(PlanContract.builder().methodProbes(3).build())
                .hits(HitsContract.atLeastMethods(2, false, false))
                .args(ArgsContract.builder()
                        .method("greet", List.of(List.of("world", "1")))
                        .build())
                .skipInvocations("single call")
                .skipAttribution("single execution")
                .build();

    }
}

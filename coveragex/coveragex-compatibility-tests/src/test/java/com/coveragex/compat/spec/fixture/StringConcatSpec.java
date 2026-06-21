package com.coveragex.compat.spec.fixture;

import com.coveragex.compat.contract.HitsContract;
import com.coveragex.compat.contract.PlanContract;
import com.coveragex.compat.spec.FixtureContractSpec;
import com.coveragex.compat.spec.FixtureContracts;
import com.coveragex.compat.contract.ArgsContract;
import java.util.List;

/**
 * Spec for {@code com.coveragex.fixtures.StringConcat}.
 *
 * <p>Migrated from the legacy {@code StringConcatContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class StringConcatSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "com.coveragex.fixtures.StringConcat";
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

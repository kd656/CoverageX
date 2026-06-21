package com.coveragex.compat.spec.fixture;

import com.coveragex.compat.contract.HitsContract;
import com.coveragex.compat.contract.PlanContract;
import com.coveragex.compat.spec.FixtureContractSpec;
import com.coveragex.compat.spec.FixtureContracts;
import com.coveragex.compat.contract.ArgsContract;
import java.util.List;

/**
 * Spec for {@code com.coveragex.fixtures.InstanceFns}.
 *
 * <p>Migrated from the legacy {@code InstanceFnsContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class InstanceFnsSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "com.coveragex.fixtures.InstanceFns";
    }

    @Override
    public FixtureContracts contracts() {
        return FixtureContracts.builder()
                .plan(PlanContract.builder().methodProbes(3).build())
                .hits(HitsContract.atLeastMethods(3, false, false))
                .args(ArgsContract.builder()
                        .method("<init>", List.of(List.of("hello")))
                        .method("greet", List.of(List.of("world")))
                        .build())
                .skipInvocations("each method called once")
                .skipAttribution("composite ctor + greet action; no meaningful per-test split")
                .build();

    }
}

package com.coveragex.compat.spec.fixture;

import com.coveragex.compat.contract.HitsContract;
import com.coveragex.compat.contract.PlanContract;
import com.coveragex.compat.spec.FixtureContractSpec;
import com.coveragex.compat.spec.FixtureContracts;
import com.coveragex.compat.contract.ArgsContract;
import java.util.List;

/**
 * Spec for {@code com.coveragex.fixtures.AssertStmt}.
 *
 * <p>Migrated from the legacy {@code AssertStmtContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class AssertStmtSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "com.coveragex.fixtures.AssertStmt";
    }

    @Override
    public FixtureContracts contracts() {
        return FixtureContracts.builder()
                .plan(PlanContract.builder().methodProbes(4).build())
                .hits(HitsContract.atLeastMethods(3, false, false))
                .args(ArgsContract.builder()
                        .method("positive", List.of(List.of("5")))
                        .build())
                .skipInvocations("single call")
                .skipAttribution("single execution")
                .build();

    }
}

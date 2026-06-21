package io.github.kd656.coveragex.compat.spec.fixture;

import io.github.kd656.coveragex.compat.contract.HitsContract;
import io.github.kd656.coveragex.compat.contract.PlanContract;
import io.github.kd656.coveragex.compat.spec.FixtureContractSpec;
import io.github.kd656.coveragex.compat.spec.FixtureContracts;

/**
 * Spec for {@code io.github.kd656.coveragex.fixtures.VarForEach}.
 *
 * <p>Migrated from the legacy {@code VarForEachContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class VarForEachSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "io.github.kd656.coveragex.fixtures.VarForEach";
    }

    @Override
    public FixtureContracts contracts() {
        return FixtureContracts.builder()
                .plan(PlanContract.builder().methodProbes(3).build())
                .hits(HitsContract.atLeastMethods(2, false, false))
                .skipArgs("List<String> arg; collection toString may shift across JDKs")
                .skipInvocations("single outer call")
                .skipAttribution("single execution")
                .build();

    }
}

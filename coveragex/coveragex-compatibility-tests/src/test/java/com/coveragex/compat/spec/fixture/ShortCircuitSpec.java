package com.coveragex.compat.spec.fixture;

import com.coveragex.api.data.ProbeMetadata.BranchDirection;
import com.coveragex.compat.contract.HitsContract;
import com.coveragex.compat.contract.PlanContract;
import com.coveragex.compat.spec.FixtureContractSpec;
import com.coveragex.compat.spec.FixtureContracts;
import java.util.List;
import com.coveragex.compat.contract.ArgsContract;

/**
 * Spec for {@code com.coveragex.fixtures.ShortCircuit}.
 *
 * <p>Migrated from the legacy {@code ShortCircuitContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class ShortCircuitSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "com.coveragex.fixtures.ShortCircuit";
    }

    @Override
    public FixtureContracts contracts() {
        return FixtureContracts.builder()
                .plan(PlanContract.builder()
                .methodProbes(4)        // <init> + both + either + execute
                .anyLineBranch(BranchDirection.TRUE)
                .anyLineBranch(BranchDirection.TRUE)
                .anyLineBranch(BranchDirection.TRUE)
                .anyLineBranch(BranchDirection.TRUE)
                .anyLineBranch(BranchDirection.FALSE)
                .anyLineBranch(BranchDirection.FALSE)
                .anyLineBranch(BranchDirection.FALSE)
                .anyLineBranch(BranchDirection.FALSE)
                .build())
                .hits(new HitsContract(3, 0, 0, true, true, List.of(), List.of()))
                .args(ArgsContract.builder()
                        .method("both", List.of(List.of("1", "1"), List.of("1", "0"), List.of("0", "0")))
                        .method("either", List.of(List.of("0", "1"), List.of("1", "0")))
                        .build())
                .skipInvocations("each truth-table call once")
                .skipAttribution("five short-circuit variants; args dimension already encodes the input combinations")
                .build();

    }
}

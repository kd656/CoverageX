package io.github.kd656.coveragex.compat.spec.fixture;

import io.github.kd656.coveragex.api.data.ProbeMetadata.BranchDirection;
import io.github.kd656.coveragex.compat.contract.HitsContract;
import io.github.kd656.coveragex.compat.contract.PlanContract;
import io.github.kd656.coveragex.compat.spec.FixtureContractSpec;
import io.github.kd656.coveragex.compat.spec.FixtureContracts;
import java.util.List;
import io.github.kd656.coveragex.compat.contract.ArgsContract;

/**
 * Spec for {@code io.github.kd656.coveragex.fixtures.ShortCircuit}.
 *
 * <p>Migrated from the legacy {@code ShortCircuitContracts} utility class. Original
 * commentary and contract values preserved verbatim from the source.</p>
 */
public final class ShortCircuitSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "io.github.kd656.coveragex.fixtures.ShortCircuit";
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

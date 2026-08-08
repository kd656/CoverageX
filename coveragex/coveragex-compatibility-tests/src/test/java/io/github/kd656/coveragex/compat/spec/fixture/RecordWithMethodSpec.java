package io.github.kd656.coveragex.compat.spec.fixture;

import io.github.kd656.coveragex.api.data.ProbeMetadata.BranchDirection;
import io.github.kd656.coveragex.compat.contract.ArgsContract;
import io.github.kd656.coveragex.compat.contract.HitsContract;
import io.github.kd656.coveragex.compat.contract.HitsContract.BranchHitExpectation;
import io.github.kd656.coveragex.compat.contract.InvocationContract;
import io.github.kd656.coveragex.compat.contract.PlanContract;
import io.github.kd656.coveragex.compat.spec.FixtureContractSpec;
import io.github.kd656.coveragex.compat.spec.FixtureContracts;

import java.util.List;

/**
 * Spec for a top-level Java record with explicit behavior.
 */
public final class RecordWithMethodSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "io.github.kd656.coveragex.fixtures.RecordWithMethod";
    }

    @Override
    public FixtureContracts contracts() {
        return FixtureContracts.builder()
                .plan(PlanContract.builder()
                        // Three probes: <init>, adult, execute. The synthetic age()
                        // accessor is filtered out by RecordMethods.
                        .methodProbes(3)
                        .branch(6, BranchDirection.TRUE)
                        .branch(6, BranchDirection.FALSE)
                        .build())
                .hits(new HitsContract(
                        3, 0, 0, true, true,
                        List.of(
                                BranchHitExpectation.atLeastOnce(6, BranchDirection.TRUE),
                                BranchHitExpectation.atLeastOnce(6, BranchDirection.FALSE)),
                        List.of()))
                .args(ArgsContract.builder()
                        .method("adult", List.of(List.of()))
                        .method("<init>", List.of(List.of("20"), List.of("10")))
                        .build())
                .invocations(InvocationContract.builder()
                        .method("adult", 2)
                        .method("<init>", 2)
                        .method("execute", 1)
                        .build())
                .skipAttribution("adult is an instance record method; runner invocation plans drive static methods only")
                .build();
    }
}

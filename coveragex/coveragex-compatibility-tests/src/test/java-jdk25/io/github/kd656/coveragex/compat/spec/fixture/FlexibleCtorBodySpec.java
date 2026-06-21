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
 * Spec for {@code io.github.kd656.coveragex.fixtures.FlexibleCtorBody}.
 *
 * <p>First forward-only fixture in the matrix — exercises JDK 25 flexible
 * constructor bodies (JEP 513). javac 21 cannot parse the source; the
 * matrix verifies that the analyzer + recorder still produce sensible
 * contracts when javac 25 emits the pre-this() bytecode.</p>
 *
 * <p>{@code execute()} drives:
 * <ul>
 *   <li>{@code new FlexibleCtorBody(-5)} — pre-this() normalizes to 0 (ternary TRUE arm)</li>
 *   <li>{@code new FlexibleCtorBody(10)} — pre-this() keeps 10 (ternary FALSE arm)</li>
 * </ul>
 * Both branch directions on line 24 fire. The chained {@code this(normalized, true)}
 * lands in {@code <init>(int, boolean)} twice, contributing additional entries
 * to the {@code <init>} method-probe.</p>
 */
public final class FlexibleCtorBodySpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "io.github.kd656.coveragex.fixtures.FlexibleCtorBody";
    }

    @Override
    public FixtureContracts contracts() {
        return FixtureContracts.builder()
                // 4 method probes: <init>(int), <init>(int,boolean), value(), execute().
                // Ternary on line 24 produces TRUE+FALSE branches.
                .plan(PlanContract.builder()
                        .methodProbes(4)
                        .branch(24, BranchDirection.TRUE)
                        .branch(24, BranchDirection.FALSE)
                        .build())
                // <init>(int), <init>(int,boolean), execute fire; value() doesn't.
                // 3 minimum method hits; both branch directions firing.
                .hits(new HitsContract(
                        /* minMethodHits             */ 3,
                        /* minReturnHits             */ 1,
                        /* minThrowHits              */ 0,
                        /* requireTrueBranchHit      */ true,
                        /* requireFalseBranchHit     */ true,
                        List.of(
                                BranchHitExpectation.atLeastOnce(24, BranchDirection.TRUE),
                                BranchHitExpectation.atLeastOnce(24, BranchDirection.FALSE)),
                        List.of()))
                // Both <init> overloads share methodName "<init>"; combined captures:
                //   <init>(int)        : [["-5"], ["10"]]
                //   <init>(int,boolean): [["0","true"], ["10","true"]]
                .args(ArgsContract.builder()
                        .method("<init>", List.of(
                                List.of("-5"),
                                List.of("10"),
                                List.of("0", "true"),
                                List.of("10", "true")))
                        .build())
                // <init> probes fire 4 times total: 2 from execute()'s 'new'
                // expressions + 2 from the chained this(normalized, true) calls.
                .invocations(InvocationContract.builder()
                        .method("<init>", 4)
                        .method("execute", 1)
                        .build())
                // Same blocker as RecordCompactCtor: per-test attribution would
                // require driving each ctor reflectively via Constructor.newInstance,
                // which the current runner does not support. Revisit when ctor
                // reflection lands.
                .skipAttribution("requires ctor reflection in the runner (see RecordCompactCtorSpec)")
                .build();
    }
}

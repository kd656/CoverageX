package com.coveragex.compat.spec.fixture;

import com.coveragex.compat.contract.HitsContract;
import com.coveragex.compat.contract.PlanContract;
import com.coveragex.compat.spec.FixtureContractSpec;
import com.coveragex.compat.spec.FixtureContracts;

/**
 * Spec for {@code com.coveragex.fixtures.UnnamedPatternInSwitch}.
 *
 * <p>First forward fixture in the JDK 22 module — exercises JEP 456 unnamed
 * patterns ({@code case Circle _ -> ...}). The wildcard binding allocates
 * no local-variable slot, producing slightly different bytecode than the
 * existing {@code SealedTypesSpec} fixture which binds the case value.</p>
 *
 * <p>Contracts kept conservative: pattern matching for switch is JDK-fragile,
 * so we pin only what is structurally invariant — the outer method probe
 * count and the minimum number of method hits. The pattern-switch's branch
 * shape is intentionally not pinned, matching the loose stance of the
 * existing {@code SealedTypesSpec} / {@code PatternSealedSwitchSpec}.</p>
 */
public final class UnnamedPatternInSwitchSpec implements FixtureContractSpec {

    @Override
    public String fqn() {
        return "com.coveragex.fixtures.UnnamedPatternInSwitch";
    }

    @Override
    public FixtureContracts contracts() {
        return FixtureContracts.builder()
                // Outer class methods: <init> + name(Shape) + execute()
                // = 3 method probes on the outer (nested Circle/Square/Shape are separate class files).
                .plan(PlanContract.builder().methodProbes(3).build())
                // name() and execute() fire; <init> not invoked.
                .hits(HitsContract.atLeastMethods(2, false, false))
                .skipArgs("args are Circle/Square records with structured toString that may shift across JDKs")
                .skipInvocations("each switch arm hit once")
                .skipAttribution("requires constructing Circle/Square records reflectively in the plan; same blocker as SealedTypesSpec and PatternSealedSwitchSpec")
                .build();
    }
}

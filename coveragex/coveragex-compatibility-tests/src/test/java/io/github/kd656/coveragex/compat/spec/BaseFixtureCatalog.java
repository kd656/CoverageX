package io.github.kd656.coveragex.compat.spec;

import io.github.kd656.coveragex.compat.spec.fixture.*;

import java.util.List;

/**
 * The base fixture catalog — every fixture whose source compiles on the
 * current matrix floor JDK.
 *
 * <p>Adding a fixture whose syntax the floor JDK can parse: write its
 * {@code *Spec.java} under {@code spec/fixture/} and add one line below.
 * Removing a fixture: delete the line. This file is the source of truth
 * for the floor-row matrix.</p>
 *
 * <p>Forward-only fixtures (syntax requiring a newer JDK) do not appear
 * here — they live in their own {@code Jdk<N>FixtureCatalog} contributors
 * that are conditionally compiled and discovered via
 * {@link FixtureCatalogContributor}.</p>
 */
public final class BaseFixtureCatalog {

    private BaseFixtureCatalog() {}

    private static final List<FixtureContractSpec> SPECS = List.of(
            // Tier A — method shapes
            new StaticFnsSpec(),
            new InstanceFnsSpec(),
            new VoidFnSpec(),
            new ReturnPrimSpec(),
            new ReturnRefSpec(),
            new RecursiveSpec(),
            new CtorDefaultSpec(),
            new CtorOverloadedSpec(),

            // Tier A — conditionals
            new IfElseSpec(),
            new IfElseIfElseSpec(),
            new TernarySpec(),
            new ShortCircuitSpec(),
            new NullCheckSpec(),

            // Tier A — loops
            new ForLoopSpec(),
            new ForEachArraySpec(),
            new ForEachIterableSpec(),
            new WhileDoSpec(),
            new DoWhileSpec(),

            // Tier A — switches
            new SwitchIntSpec(),
            new SwitchStringSpec(),

            // Tier A — exceptions
            new TryCatchSpec(),
            new TryCatchFinallySpec(),
            new TryWithResourcesSpec(),
            new MultiCatchSpec(),

            // Tier A — references and inner constructs
            new LambdaSpec(),
            new MethodRefSpec(),
            new AnonClassSpec(),
            new InnerClassSpec(),

            // Tier A — initializers and misc
            new StaticInitSpec(),
            new InstanceInitSpec(),
            new StringConcatSpec(),
            new AssertStmtSpec(),

            // Tier B
            new SwitchExprArrowSpec(),
            new SwitchExprYieldSpec(),
            new TextBlockSpec(),
            new RecordSimpleSpec(),
            new RecordCompactCtorSpec(),
            new RecordWithMethodSpec(),
            new SealedTypesSpec(),
            new PatternInstanceofSpec(),
            new PatternSwitchSpec(),
            new RecordPatternSpec(),
            new VarLocalSpec(),
            new VarForEachSpec(),
            new VirtualThreadSpec(),

            // Tier C
            new NestedIfForSpec(),
            new TryInLambdaSpec(),
            new SwitchInCatchSpec(),
            new LambdaCaptureLoopSpec(),
            new RecursiveLambdaSpec(),
            new TwrTwoResourcesSpec(),
            new SwitchExprReturnSpec(),
            new PatternSealedSwitchSpec(),
            new ForEachRecordPatternSpec(),
            new StaticInitTryCatchSpec(),
            new AnonInLambdaSpec(),
            new NestedTwrSpec()
    );

    public static List<FixtureContractSpec> specs() {
        return SPECS;
    }
}

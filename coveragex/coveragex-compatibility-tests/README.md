# coveragex-compatibility-tests

The JDK-upgrade safety module. Runs every fixture in `coveragex-test-fixtures`
through both the static planner and the runtime collector, asserting
per-fixture contracts on every supported JDK.

## What this module is

A test-only module — there is no `src/main/java`. All code lives under
`src/test/java`. The module's job is to **execute the matrix**: for each
fixture × each supported JDK row, prove the static probe plan still has
the expected shape and the runtime collector still records the expected
hits.

When you bump the JDK from 21 → 22 → 25 (or a new GA), this module is
where you find out what changed. Green matrix = ship. Red row = either
fix the analyzer or loosen the contract, with the failure message
naming exactly which fixture and which assertion drifted.

## Layout

```
src/test/java/com/coveragex/compat/
├── ProbePlanByConstructTest.java        Parameterized — static plan check
├── ProbeHitsByConstructTest.java        Parameterized — runtime hits check
├── contract/
│   ├── PlanContract.java                Static-plan contract (record + builder)
│   ├── HitsContract.java                Runtime-hit contract (record + factory)
│   ├── BranchExpectation.java           Sealed — pinned line vs any-line
│   └── fixture/
│       ├── IfElseContracts.java         One per fixture — plan() + hits()
│       ├── ...                          
│       └── NestedTwrContracts.java
└── testutil/
    ├── BytecodeFixtures.java            Loads fixture .class bytes + nested classes
    ├── ProbeMetadataIndex.java          probeId → ProbeMetadata lookup for HitsContract
    ├── ByteArrayClassLoader.java        Child-first loader for instrumented bytes
    ├── PlanFormatter.java               Deterministic text dump
    ├── PlanDumpOnFailure.java           JUnit extension — dumps plans on failure
    ├── JdkVersion.java                  Reads fixture.jdk system property
    ├── EnabledOnFixtureJdk.java         Annotation gating tests by JDK range
    └── EnabledOnFixtureJdkCondition.java  ExecutionCondition impl
```

## The two runners

Each runner is a single `@ParameterizedTest` driven by a `@MethodSource("fixtures")`
that returns one `Arguments.of(fixtureFqn, contract)` per fixture.

### `ProbePlanByConstructTest`

For each fixture:
1. Load raw bytes from the classpath via `BytecodeFixtures.load(fqn)`.
2. Run them through `ProbePlanBuilder.build(...)`.
3. Assert the resulting `ProbePlan` satisfies the fixture's `PlanContract`.

This verifies the *static* analysis — what the agent would emit if it
were instrumenting this class. Catches probe-ID drift, metadata
emission-order regressions, synthetic-method count changes between JDKs.

### `ProbeHitsByConstructTest`

For each fixture:
1. Load raw bytes (including nested classes via `loadWithNestedClasses`).
2. Run them through `ClassTransformer` to get instrumented bytes.
3. Define the result in a child-first `ByteArrayClassLoader`.
4. Invoke `execute()` via reflection.
5. Read hits from `CommonCoverageDataCollector.getProbeData(classId)` and
   assert the fixture's `HitsContract`.

This verifies the *runtime* path — instrumentation + collector + recording.
Catches transformer regressions, broken `INVOKESTATIC` emission, lost
call sequences. A fresh `CommonCoverageDataCollector` is installed via
`CoverageDataCollectorDelegate.setCollector(...)` per test method
(@`BeforeEach`) so state cannot leak between iterations.

## How a contract is structured

```java
public final class IfElseContracts {
    public static PlanContract plan() {
        return PlanContract.builder()
            .methodProbes(3)
            .branch(6, BranchDirection.TRUE)
            .branch(6, BranchDirection.FALSE)
            .returnsOnLines(3, 7, 9, 15)
            .build();
    }

    public static HitsContract hits() {
        return new HitsContract(
            /* minMethodHits */ 2,
            /* minReturnHits */ 2,
            /* minThrowHits */ 0,
            /* requireTrueBranchHit */ true,
            /* requireFalseBranchHit */ true,
            List.of(...), List.of());
    }
}
```

Most contracts are intentionally *loose* — they pin the shape that
must hold regardless of JDK desugaring tweaks, and skip dimensions
where JDK drift is expected. See `PlanContract.java` and
`HitsContract.java` for the full vocabulary.

## When a contract fails

`PlanDumpOnFailure` writes the actual plan to
`target/plan-dumps/jdk-<NN>/<fixtureFqn>.plan.txt` only when the test
fails. The CI workflow uploads that directory as an artifact named
`plan-dumps-<profile>`. To iterate locally:

```sh
mvn -pl coveragex-compatibility-tests test
cat target/plan-dumps/jdk-21/com.coveragex.fixtures.SomeFixture.plan.txt
# refine the contract → re-run → repeat
```

## Build commands

```sh
# Default profile (JDK 21 row)
mvn -pl coveragex-compatibility-tests -am test

# JDK 25 row
mvn -Pfixtures-jdk25 -pl coveragex-compatibility-tests -am test

# JDK 22 row (requires JDK 22 in ~/.m2/toolchains.xml)
mvn -Pfixtures-jdk22 -pl coveragex-compatibility-tests -am test

# Single test class
mvn -pl coveragex-compatibility-tests test -Dtest=ProbePlanByConstructTest
```

## Dependencies

| Dep | Why |
|---|---|
| `coveragex-core` | `ProbePlanBuilder`, `ClassTransformer`, `CommonCoverageDataCollector`, `CoverageDataCollectorDelegate` (api comes transitively) |
| `coveragex-test-fixtures` | the actual `.class` files on the test classpath |
| `junit-jupiter` | aggregator — bundles `-api`, `-params`, `-engine` |
| `assertj-core` | `SoftAssertions` in the contracts |

Notably absent: **no `coveragex-agent` dependency**. The runtime engine
lives in `coveragex-core` after the agent-relocation refactor (see
`documentation/coveragex-core-test-strategy-impl.md` §10.4). The agent
module only contains JVM-attachment plumbing (`CoverageAgent.premain`).

## Related docs

- `../CONTRIBUTING.md` — toolchains setup, JDK floor policy.

# coveragex-test-fixtures

Java-construct fixtures consumed by `coveragex-compatibility-tests`.

## What this module is

A library of small Java classes — one per language construct — that exist
**solely** to be instrumented and executed by the compatibility test suite.
Each class exposes a single `public static void execute()` method that
exercises the construct with deterministic inputs.

The module produces a regular jar of compiled `.class` files. It is **not**
intended for end-user consumption; it lives in the reactor exclusively to
feed bytecode into `coveragex-compatibility-tests`.

## What it contains

`src/main/java/com/coveragex/fixtures/` holds one `.java` per fixture, organized into three tiers:

| Tier | Examples | Purpose |
|---|---|---|
| **Tier A** (baseline) | `IfElse`, `ForLoop`, `TryCatch`, `Lambda`, `Recursive` | Core language constructs present in every supported JDK |
| **Tier B** (version-sensitive) | `RecordSimple`, `SealedTypes`, `PatternSwitch`, `VirtualThread` | Features added in specific JDK releases — drift catchers |
| **Tier C** (combinations) | `TryInLambda`, `SwitchInCatch`, `NestedTwr` | Interaction bugs that don't appear in single constructs alone |

A fixture's source code is **the specification**. The reader should be able
to glance at the file and know exactly what bytecode shape to expect.

## How fixtures get compiled

The build is wired so the *JDK that runs `javac`* matches the active
compatibility matrix row:

- `maven-toolchains-plugin` selects the JDK via `<jdk><version>${fixture.jdk}</version></jdk>`.
- `maven-compiler-plugin` runs with `<fork>true</fork>` and `<release>${fixture.jdk}</release>`.

The `fixture.jdk` property is set by a profile in the parent POM:

| Profile | Effective JDK |
|---|---|
| `fixtures-jdk21` (default) | 21 |
| `fixtures-jdk22` | 22 |
| `fixtures-jdk25` | 25 |

For preview features, a separate per-JDK fixtures module (e.g.
`coveragex-test-fixtures-jdk22-preview/`) holds the sources that won't
parse on the baseline — none exists today.

## Adding a fixture

1. Drop a `.java` file in `src/main/java/com/coveragex/fixtures/<Name>.java`.
   It must declare `public static void execute()` driving the construct.
2. Add a `<Name>Contracts.java` factory in `coveragex-compatibility-tests`.
3. Add **two** `Arguments.of(...)` entries — one per runner.

## Build commands

```sh
# Default profile (JDK 21)
mvn -pl coveragex-test-fixtures install

# Compile fixtures with JDK 25
mvn -Pfixtures-jdk25 -pl coveragex-test-fixtures install
```

The resulting `target/coveragex-test-fixtures-*.jar` contains one
`.class` file per fixture, suitable for loading from the test classpath
via `BytecodeFixtures.load("com.coveragex.fixtures.<Name>")`.

## Related docs

- `../CONTRIBUTING.md` — toolchains setup, JDK floor policy.

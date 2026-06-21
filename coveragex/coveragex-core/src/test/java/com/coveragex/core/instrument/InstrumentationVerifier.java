package com.coveragex.core.instrument;

import com.coveragex.core.collect.CoverageDataCollectorDelegate;
import com.coveragex.core.collect.CommonCoverageDataCollector;
import com.coveragex.core.instrument.ClassTransformer;
import com.coveragex.api.data.ClassCoverage;
import com.coveragex.api.data.ExecutionData;
import com.coveragex.api.data.ProbeMetadata;
import com.coveragex.api.io.internal.BinaryDataWriter;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Test harness for probe injection verification.
 *
 * The challenge: ClassTransformer.transform() returns raw byte[] — you cannot call
 * a method on bytes. To actually run the instrumented code and verify probes fire,
 * the transformed bytes must be turned into a real Class<?> via ClassLoader.defineClass().
 * That is the only Java API that converts bytecode into something executable.
 *
 * Why not call the fixture directly?
 * The fixture class (e.g. SimpleIf) is already loaded by the system classloader when
 * the test JVM starts. A class loaded by a different classloader is treated as a
 * completely separate type — casting it to SimpleIf would throw ClassCastException.
 * Reflection bypasses this: it dispatches into the instrumented bytecode without
 * requiring a compile-time type reference.
 *
 * Lifecycle per test:
 *   1. Read original .class bytes from classpath
 *   2. Run ClassTransformer — probe calls are injected, ProbeMetadata is registered
 *   3. Load transformed bytes via a child ClassLoader (defineClass)
 *   4. Instantiate via reflection, expose invoke() for test-driven execution
 *   5. After invoke(), query collector for probe hits and assert
 */
public class InstrumentationVerifier {

    /** The collector that receives recordHit() calls from injected probe bytecode. */
    private final CommonCoverageDataCollector collector;

    /**
     * JVM internal class name in slash notation (e.g. "com/example/SimpleIf").
     * Used as the key into the collector's probe maps.
     */
    private final String classId;

    /** Instance of the instrumented class, created via reflection after dynamic loading. */
    private final Object instance;

    /**
     * The instrumented Class<?> object, loaded by a child ClassLoader so it is
     * isolated from the already-loaded original version of the same class.
     */
    private final Class<?> instrumentedClass;

    /**
     * Instruments and loads the given fixture class.
     */
    private InstrumentationVerifier(Class<?> fixtureClass) throws Exception {
        collector = new CommonCoverageDataCollector(new BinaryDataWriter());
        collector.reset();
        // Register this collector as the global target for recordHit() calls.
        // The injected bytecode calls CoverageDataCollectorDelegate.recordHit() statically,
        // so the delegate must point at this instance before the instrumented code runs.
        CoverageDataCollectorDelegate.registry().installGlobal(collector);

        classId = fixtureClass.getName().replace('.', '/');
        String resourcePath = classId + ".class";

        byte[] originalBytes;
        try (var stream = fixtureClass.getClassLoader().getResourceAsStream(resourcePath)) {
            assertThat(stream).as("Fixture .class not found: %s", resourcePath).isNotNull();
            originalBytes = stream.readAllBytes();
        }

        // Include the fixture's package so ClassTransformer does not filter it out.
        // Excludes are empty — the default "com.coveragex.**" exclude is only added
        // when the includes list is empty, which we avoid here.
        ClassTransformer transformer = new ClassTransformer(
            collector,
            List.of(fixtureClass.getPackageName() + ".**"),
            List.of(),
            null   // no coverage map — uses DefaultProbeInjector
        );

        byte[] transformed = transformer.transform(
            fixtureClass.getClassLoader(), classId, null, null, originalBytes
        );
        assertThat(transformed).as("Transformer returned null for %s", classId).isNotNull();

        // Child ClassLoader: intercepts only the fixture class and defines it from
        // the instrumented bytes. Everything else delegates to the parent so that
        // standard library types remain shared and castable.
        ClassLoader loader = new ClassLoader(fixtureClass.getClassLoader()) {
            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                if (name.equals(fixtureClass.getName())) {
                    // Avoid redefining if already loaded in this loader's namespace.
                    Class<?> cached = findLoadedClass(name);
                    if (cached != null) return cached;
                    return defineClass(name, transformed, 0, transformed.length);
                }
                return super.loadClass(name);
            }
        };

        instrumentedClass = loader.loadClass(fixtureClass.getName());
        instance = instrumentedClass.getDeclaredConstructor().newInstance();
    }

    /**
     * Factory method. Creates a verifier for the given fixture class:
     * instruments it, loads the instrumented version, and instantiates it.
     * Call this at the start of each test.
     */
    public static InstrumentationVerifier of(Class<?> fixtureClass) throws Exception {
        return new InstrumentationVerifier(fixtureClass);
    }

    // -------------------------------------------------------------------------
    // Invocation
    // -------------------------------------------------------------------------

    /**
     * Invokes a method on the instrumented instance via reflection.
     *
     * Reflection is required because the instrumented class was loaded by a child
     * ClassLoader and is a different type from the compile-time fixture class.
     * There is no shared type to cast to — reflection is the only dispatch path
     * that works across classloader boundaries.
     *
     * @param methodName  simple method name as it appears in source
     * @param paramTypes  parameter types used to resolve overloads
     * @param args        arguments to pass; must match paramTypes in order
     */
    public Object invoke(String methodName, Class<?>[] paramTypes, Object... args) throws Exception {
        Method m = instrumentedClass.getDeclaredMethod(methodName, paramTypes);
        m.setAccessible(true);
        return m.invoke(instance, args);
    }

    /**
     * Convenience overload for no-arg methods.
     */
    public Object invoke(String methodName) throws Exception {
        return invoke(methodName, new Class[0]);
    }

    /**
     * Invokes a static method on the instrumented class via reflection.
     *
     * @param methodName  simple method name as it appears in source
     * @param paramTypes  parameter types used to resolve overloads
     * @param args        arguments to pass; must match paramTypes in order
     */
    public Object invokeStatic(String methodName, Class<?>[] paramTypes, Object... args) throws Exception {
        Method m = instrumentedClass.getDeclaredMethod(methodName, paramTypes);
        m.setAccessible(true);
        return m.invoke(null, args);   // null = no instance for static methods
    }

    // -------------------------------------------------------------------------
    // Probe data accessors
    // -------------------------------------------------------------------------

    /**
     * Returns the ProbeMetadata list registered for the instrumented class.
     * This is populated by ProbeInjector during transformation (before any method runs)
     * and describes every probe that was injected: its ID, type, method name, and
     * source line. Use this to understand what was injected before asserting on hits.
     */
    public List<ProbeMetadata> probeMetadata() {
        ExecutionData snapshot = collector.snapshot();
        ClassCoverage cc = snapshot.classCoverage(classId);
        assertThat(cc).as("Class not registered: %s", classId).isNotNull();
        return cc.probeMetadata();
    }

    /**
     * Returns the raw probe hit array for the instrumented class.
     * Index i is true if probe with probeId==i was hit at least once.
     * This is the low-level data that all assertions are built on.
     */
    public boolean[] probeHits() {
        return collector.getProbeData(classId);
    }

    /**
     * Returns the number of branch pairs injected into the named method.
     * Counts TRUE-direction BranchProbes only (one per pair) to avoid double-counting.
     * Use this as a structural assertion: "this method has exactly N branch points".
     */
    public int countBranchPairsInMethod(String methodName) {
        return (int) probeMetadata().stream()
            .filter(pm -> pm instanceof ProbeMetadata.BranchProbe bp
                && methodName.equals(bp.methodName())
                && bp.direction() == ProbeMetadata.BranchDirection.TRUE)
            .count();
    }

    // -------------------------------------------------------------------------
    // Assertions
    // -------------------------------------------------------------------------

    /**
     * Asserts that at least one probe in the named method was hit.
     * Use this when you only care that execution reached the method,
     * not which specific path was taken (e.g. catch blocks, finally).
     */
    public void assertMethodEntered(String methodName) {
        boolean[] hits = probeHits();
        boolean any = probeMetadata().stream()
            .filter(pm -> methodName.equals(pm.methodName()))
            .anyMatch(pm -> hits[pm.probeId()]);
        assertThat(any).as("No probe hit in method %s", methodName).isTrue();
    }

    /**
     * Asserts that no probe in the named method was hit.
     * Use this to verify that a method was never called — for example, a lambda
     * body that should not execute on an empty input list.
     */
    public void assertMethodNotEntered(String methodName) {
        boolean[] hits = probeHits();
        boolean any = probeMetadata().stream()
            .filter(pm -> methodName.equals(pm.methodName()))
            .anyMatch(pm -> hits[pm.probeId()]);
        assertThat(any).as("Expected no probe hit in %s but some were hit", methodName).isFalse();
    }

    /**
     * Asserts that every probe injected into the named method was hit.
     * This is the strongest coverage assertion: method entry, all branch directions,
     * all line probes, and all segments must have fired. Useful for verifying that
     * a set of invocations achieves complete method-level coverage.
     */
    public void assertFullMethodCoverage(String methodName) {
        boolean[] hits = probeHits();
        probeMetadata().stream()
            .filter(pm -> methodName.equals(pm.methodName()))
            .forEach(pm -> assertThat(hits[pm.probeId()])
                .as("Probe %d (%s) in %s not hit", pm.probeId(), pm, methodName)
                .isTrue());
    }

    /**
     * Asserts that the branch probe at the given source line and direction was hit.
     * The line parameter is required whenever a method contains more than one branch
     * pair — for example, a method with && or || or nested ifs produces multiple
     * BranchProbes with the same direction, and the line disambiguates which one.
     *
     * @param methodName  method containing the branch
     * @param line        source line of the conditional (the line with if/while/&&/etc.)
     * @param direction   "TRUE" or "FALSE"
     */
    public void assertBranchHit(String methodName, int line, String direction) {
        assertBranch(methodName, line, direction, true);
    }

    /** @see #assertBranchHit(String, int, String) */
    public void assertBranchNotHit(String methodName, int line, String direction) {
        assertBranch(methodName, line, direction, false);
    }

    /**
     * Convenience overload for methods with exactly one branch pair.
     * Fails with a descriptive message if the method has multiple branch pairs —
     * use assertBranchHit(method, line, direction) in that case.
     */
    public void assertBranchHit(String methodName, String direction) {
        assertBranch(methodName, -1, direction, true);
    }

    /** @see #assertBranchHit(String, String) */
    public void assertBranchNotHit(String methodName, String direction) {
        assertBranch(methodName, -1, direction, false);
    }

    /**
     * Asserts the exact number of branch pairs injected into the method.
     * This is a structural test — it verifies the injector produced the right number
     * of branch probes before any code runs. Use it as a precondition assertion to
     * catch injector regressions early (e.g. && suddenly producing 1 pair instead of 2).
     */
    public void assertBranchPairCount(String methodName, int expectedPairs) {
        assertThat(countBranchPairsInMethod(methodName))
            .as("Expected %d branch pairs in %s", expectedPairs, methodName)
            .isEqualTo(expectedPairs);
    }

    /**
     * Resets the collector state between test runs within the same test method.
     * Use this when a single test invokes the same method multiple times and wants
     * to check probe state independently for each invocation.
     */
    public void reset() {
        collector.reset();
    }

    /**
     * Core branch assertion implementation.
     * Finds the BranchProbe matching (methodName, line, direction), then checks
     * whether probeHits[probeId] equals the expected value.
     *
     * line == -1 means "any line" — only safe when the method has one branch pair.
     */
    private void assertBranch(String methodName, int line, String direction, boolean expected) {
        List<ProbeMetadata.BranchProbe> candidates = probeMetadata().stream()
            .filter(pm -> pm instanceof ProbeMetadata.BranchProbe bp
                && methodName.equals(bp.methodName())
                && (line == -1 || bp.line() == line)
                && direction.equals(bp.direction().name()))
            .map(pm -> (ProbeMetadata.BranchProbe) pm)
            .toList();

        if (candidates.isEmpty()) {
            fail("No BranchProbe found in method=%s line=%d direction=%s", methodName, line, direction);
        }
        if (candidates.size() > 1 && line == -1) {
            fail("Multiple BranchProbes for direction=%s in %s — use assertBranchHit(method, line, direction)",
                direction, methodName);
        }

        boolean[] hits = probeHits();
        ProbeMetadata.BranchProbe probe = candidates.get(0);
        assertThat(hits[probe.probeId()])
            .as("Probe %d [%s %s line=%d] expected hit=%b", probe.probeId(), methodName, direction, probe.line(), expected)
            .isEqualTo(expected);
    }
}

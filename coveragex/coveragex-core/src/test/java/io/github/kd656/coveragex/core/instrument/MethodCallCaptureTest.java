package io.github.kd656.coveragex.core.instrument;

import io.github.kd656.coveragex.api.context.ContextKey;
import io.github.kd656.coveragex.api.context.ProbeExecutionContext;
import io.github.kd656.coveragex.api.context.StandardContextKeys;
import io.github.kd656.coveragex.api.data.AttributedInvocation;
import io.github.kd656.coveragex.api.data.ClassCoverage;
import io.github.kd656.coveragex.api.data.ProbeMetadata;
import io.github.kd656.coveragex.api.io.internal.BinaryDataWriter;
import io.github.kd656.coveragex.core.analysis.source.impl.SourceCodeAnalyzer;
import io.github.kd656.coveragex.core.analysis.source.model.ClassModel;
import io.github.kd656.coveragex.core.analysis.source.model.SemanticIndex;
import io.github.kd656.coveragex.core.collect.CommonCoverageDataCollector;
import io.github.kd656.coveragex.core.collect.CoverageDataCollectorDelegate;
import io.github.kd656.coveragex.core.instrument.stubs.AccessService;
import io.github.kd656.coveragex.core.instrument.stubs.DoubleParser;
import io.github.kd656.coveragex.core.instrument.stubs.FlagProvider;
import io.github.kd656.coveragex.core.instrument.stubs.FlagRules;
import io.github.kd656.coveragex.core.instrument.stubs.LongLimits;
import io.github.kd656.coveragex.core.instrument.stubs.MultiArgContainer;
import com.github.javaparser.JavaParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the generalised method-call operand capture (PR 2).
 *
 * <p>The test matrix exercises every combination of instance/static × arg-count ×
 * category-1/category-2 that the spill emitter must handle, plus nested-call shapes
 * and the category-2 binary-compare literal filter.</p>
 *
 * <p>Each test instruments {@link MethodCallCaptureFixtures} with the
 * {@link SourceAwareProbeInjector}, backed by a {@link ClassModel} derived from the
 * fixture's own source file. A stub context provider attributes every branch hit to
 * the fixed test name {@code "CaptureTest#run"} so we can inspect
 * {@code ClassTestCoverage.probeInvocations()} for captured values.</p>
 */
class MethodCallCaptureTest {

    private static final String FIXTURE_CLASS = MethodCallCaptureFixtures.class.getName();
    private static final String FIXTURE_INTERNAL = FIXTURE_CLASS.replace('.', '/');
    private static final String FIXTURE_RESOURCE = FIXTURE_INTERNAL + ".class";

    private CommonCoverageDataCollector collector;
    private SourceAwareProbeInjector injector;
    private ClassModel classModel;

    @BeforeEach
    void setUp() throws Exception {
        collector = new CommonCoverageDataCollector(
                new BinaryDataWriter(),
                () -> Optional.of(stubContext("CaptureTest#run")));
        collector.reset();
        CoverageDataCollectorDelegate.registry().installGlobal(collector);
        injector = new SourceAwareProbeInjector(collector);

        URL classUrl = getClass().getClassLoader().getResource(FIXTURE_RESOURCE);
        assertThat(classUrl)
                .as("Fixture class not found on classpath: %s", FIXTURE_RESOURCE)
                .isNotNull();

        Path classFile = Paths.get(classUrl.toURI());
        Path cursor = classFile;
        while (cursor != null && !cursor.getFileName().toString().equals("target")) {
            cursor = cursor.getParent();
        }
        assertThat(cursor).as("Could not locate 'target' directory").isNotNull();

        Path srcRoot = cursor.getParent().resolve("src/test/java");
        assertThat(srcRoot).as("Test source root must exist: %s", srcRoot).exists();

        SemanticIndex index = new SemanticIndex();
        new SourceCodeAnalyzer(new JavaParser(), index).scan(srcRoot);

        classModel = index.getClasses().get(FIXTURE_CLASS);
        assertThat(classModel)
                .as("Analyser must produce a ClassModel for '%s'", FIXTURE_CLASS)
                .isNotNull();
    }

    @AfterEach
    void tearDown() {
        collector.reset();
    }

    // =========================================================================
    // Instance, 0 args — receiver captured
    // =========================================================================

    /**
     * {@code s.isBlank()} — zero args, only receiver is captured.
     * Expected labels: {@code ["s"]}. Expected value for {@code s = "abc"}: {@code ["abc"]}.
     */
    @Test
    void instanceZeroArgs_receiverCaptured() throws Exception {
        ClassCoverage cc = run("instanceZeroArgs",
                new Class[]{String.class}, new Object[]{"abc"});

        List<AttributedInvocation> invs = branchInvocations(cc);
        assertThat(invs).isNotEmpty();
        assertThat(invs).anySatisfy(inv ->
                assertThat(inv.args()).contains("abc"));

        assertLabels(cc, "s");
    }

    // =========================================================================
    // Instance, 1 ref arg with literal filtered — receiver only
    // =========================================================================

    /**
     * {@code s.startsWith(".")} — literal {@code "."} filtered, receiver captured.
     * Expected labels: {@code ["s"]}. Captured value for {@code s = "foo"}: {@code ["foo"]}.
     */
    @Test
    void instanceOneLiteralArg_receiverCaptured() throws Exception {
        ClassCoverage cc = run("instanceOneLiteralArg",
                new Class[]{String.class}, new Object[]{"foo"});

        List<AttributedInvocation> invs = branchInvocations(cc);
        assertThat(invs).isNotEmpty();
        assertThat(invs).anySatisfy(inv ->
                assertThat(inv.args()).contains("foo"));

        assertLabels(cc, "s");
    }

    // =========================================================================
    // Instance, 1 ref arg, both captured
    // =========================================================================

    /**
     * {@code a.equals(b)} — receiver and arg both captured.
     * Expected labels: {@code ["a","b"]}. Values for {@code "x","y"}: {@code ["x","y"]}.
     */
    @Test
    void instanceOneRefArgBothCaptured_bothValues() throws Exception {
        ClassCoverage cc = run("instanceOneRefArgBothCaptured",
                new Class[]{String.class, String.class}, new Object[]{"x", "y"});

        List<AttributedInvocation> invs = branchInvocations(cc);
        assertThat(invs).isNotEmpty();
        assertThat(invs).anySatisfy(inv ->
                assertThat(inv.args()).containsExactly("x", "y"));

        assertLabels(cc, "a", "b");
    }

    // =========================================================================
    // Instance, 2 ref args
    // =========================================================================

    /**
     * {@code m.contains(k, v)} — receiver and two args captured.
     * Expected labels: {@code ["m","k","v"]}. Values: {@code ["{}", "key", "val"]}.
     */
    @Test
    void instanceTwoRefArgs_allThreeCaptured() throws Exception {
        MultiArgContainer m = new MultiArgContainer("key", "val");
        ClassCoverage cc = run("instanceTwoRefArgs",
                new Class[]{MultiArgContainer.class, String.class, String.class},
                new Object[]{m, "key", "val"});

        List<AttributedInvocation> invs = branchInvocations(cc);
        assertThat(invs).isNotEmpty();
        assertThat(invs).anySatisfy(inv -> {
            assertThat(inv.args()).hasSize(3);
            assertThat(inv.args()).contains("key");
            assertThat(inv.args()).contains("val");
        });

        assertLabels(cc, "m", "k", "v");
    }

    // =========================================================================
    // Instance, long arg — category-2 spill
    // =========================================================================

    /**
     * {@code limits.isAllowed(n)} — long argument spilled via {@code LSTORE}/{@code LLOAD}.
     * Expected labels: {@code ["limits","n"]}. Values: {@code ["L{}", "100"]}.
     */
    @Test
    void instanceLongArg_receiverAndLongCaptured() throws Exception {
        LongLimits limits = new LongLimits(200L);
        ClassCoverage cc = run("instanceLongArg",
                new Class[]{LongLimits.class, long.class},
                new Object[]{limits, 100L});

        List<AttributedInvocation> invs = branchInvocations(cc);
        assertThat(invs).isNotEmpty();
        assertThat(invs).anySatisfy(inv -> {
            assertThat(inv.args()).hasSize(2);
            assertThat(inv.args()).contains("100");
        });

        assertLabels(cc, "limits", "n");
    }

    // =========================================================================
    // Instance, double arg — category-2 spill
    // =========================================================================

    /**
     * {@code parser.accepts(x)} — double argument spilled via {@code DSTORE}/{@code DLOAD}.
     * Expected labels: {@code ["parser","x"]}. Values: {@code ["P{}", "1.5"]}.
     */
    @Test
    void instanceDoubleArg_receiverAndDoubleCaptured() throws Exception {
        DoubleParser parser = new DoubleParser(1.0);
        ClassCoverage cc = run("instanceDoubleArg",
                new Class[]{DoubleParser.class, double.class},
                new Object[]{parser, 1.5});

        List<AttributedInvocation> invs = branchInvocations(cc);
        assertThat(invs).isNotEmpty();
        assertThat(invs).anySatisfy(inv -> {
            assertThat(inv.args()).hasSize(2);
            assertThat(inv.args()).contains("1.5");
        });

        assertLabels(cc, "parser", "x");
    }

    // =========================================================================
    // Instance, mixed cat-1 + cat-2 args
    // =========================================================================

    /**
     * {@code parser.acceptsMixed(off, score)} — long + double args, both spilled.
     * Expected labels: {@code ["parser","off","score"]}.
     * Values: receiver + {@code "10"} + {@code "2.5"}.
     */
    @Test
    void instanceMixedCat2Args_allThreeCaptured() throws Exception {
        DoubleParser parser = new DoubleParser(1.0);
        ClassCoverage cc = run("instanceMixedCat2Args",
                new Class[]{DoubleParser.class, long.class, double.class},
                new Object[]{parser, 10L, 2.5});

        List<AttributedInvocation> invs = branchInvocations(cc);
        assertThat(invs).isNotEmpty();
        assertThat(invs).anySatisfy(inv -> {
            assertThat(inv.args()).hasSize(3);
            assertThat(inv.args()).contains("10");
            assertThat(inv.args()).contains("2.5");
        });

        assertLabels(cc, "parser", "off", "score");
    }

    // =========================================================================
    // Static, 1 ref arg
    // =========================================================================

    /**
     * {@code StubStatics.isBlank(s)} — static call; receiver bit ignored.
     * Expected labels: {@code ["s"]}. Captured value for {@code ""}: {@code [""]}.
     */
    @Test
    void staticOneRefArg_argCaptured() throws Exception {
        ClassCoverage cc = run("staticOneRefArg",
                new Class[]{String.class}, new Object[]{""});

        List<AttributedInvocation> invs = branchInvocations(cc);
        assertThat(invs).isNotEmpty();
        assertThat(invs).anySatisfy(inv ->
                assertThat(inv.args()).contains(""));

        assertLabels(cc, "s");
    }

    // =========================================================================
    // Static, 2 ref args
    // =========================================================================

    /**
     * {@code StubStatics.matches(a, b)} — static call with two non-literal args.
     * Expected labels: {@code ["a","b"]}. Captured values: {@code ["x","y"]}.
     */
    @Test
    void staticTwoRefArgs_bothCaptured() throws Exception {
        ClassCoverage cc = run("staticTwoRefArgs",
                new Class[]{String.class, String.class}, new Object[]{"x", "y"});

        List<AttributedInvocation> invs = branchInvocations(cc);
        assertThat(invs).isNotEmpty();
        assertThat(invs).anySatisfy(inv ->
                assertThat(inv.args()).containsExactly("x", "y"));

        assertLabels(cc, "a", "b");
    }

    // =========================================================================
    // Literal-only static call — mask = 0, no capture
    // =========================================================================

    /**
     * {@code StubStatics.enabled("A")} — both scope (type ref, upper-case heuristic)
     * and arg (literal) are dropped by the analyser. Expected labels: {@code []}.
     * No values should be captured at runtime.
     *
     * <p>The class has many other methods whose branch probes carry non-empty labels;
     * this test filters to the probes belonging to {@code staticLiteralOnly} only.</p>
     */
    @Test
    void staticLiteralOnly_noCapture() throws Exception {
        ClassCoverage cc = run("staticLiteralOnly",
                new Class[]{}, new Object[]{});

        // Filter to branch probes belonging to this specific method.
        boolean anyLabels = cc.probeMetadata().stream()
                .filter(pm -> pm instanceof ProbeMetadata.BranchProbe)
                .map(pm -> (ProbeMetadata.BranchProbe) pm)
                .filter(bp -> "staticLiteralOnly".equals(bp.methodName()))
                .anyMatch(bp -> !bp.argLabels().isEmpty());
        assertThat(anyLabels)
                .as("staticLiteralOnly: literal-only static call must produce no argLabels")
                .isFalse();

        // Also verify no captured values at runtime.
        List<AttributedInvocation> invs = branchInvocations(cc);
        assertThat(invs).allSatisfy(inv ->
                assertThat(inv.args())
                        .as("literal-only call must produce no captured values")
                        .isEmpty());
    }

    // =========================================================================
    // Nested call — different name, outer wins
    // =========================================================================

    /**
     * {@code svc.canAccess(svc.resolveUser(), role)} — name guard prevents
     * premature capture on {@code resolveUser()}; outer call's args are captured.
     */
    @Test
    void nestedDifferentName_outerCallCaptured() throws Exception {
        AccessService svc = new AccessService();
        ClassCoverage cc = run("nestedDifferentName",
                new Class[]{AccessService.class, String.class},
                new Object[]{svc, "admin"});

        List<AttributedInvocation> invs = branchInvocations(cc);
        assertThat(invs).isNotEmpty();
        // The outer call has arity 2 (user + role); captured args should include "admin".
        assertThat(invs).anySatisfy(inv ->
                assertThat(inv.args()).contains("admin"));

        assertLabels(cc, "svc", "svc.resolveUser()", "role");
    }

    // =========================================================================
    // Nested boolean-returning inner — outer wins via name guard
    // =========================================================================

    /**
     * {@code rules.accepts(provider.flag(), role)} — inner {@code flag()} has
     * a different name from the outer {@code accepts()}; name guard fires correctly
     * on the outer call.
     */
    @Test
    void nestedBooleanInner_outerCallCaptured() throws Exception {
        FlagRules rules = new FlagRules();
        FlagProvider provider = new FlagProvider(false);
        ClassCoverage cc = run("nestedBooleanInner",
                new Class[]{FlagRules.class, FlagProvider.class, String.class},
                new Object[]{rules, provider, "ro"});

        List<AttributedInvocation> invs = branchInvocations(cc);
        assertThat(invs).isNotEmpty();
        // Outer call captures rules (receiver), provider.flag() (arg0), role (arg1).
        // Captured args must include "ro" (the role value).
        assertThat(invs).anySatisfy(inv ->
                assertThat(inv.args()).contains("ro"));

        assertLabels(cc, "rules", "provider.flag()", "role");
    }

    // =========================================================================
    // Category-2 binary compare with long literal — analyser filter, no capture
    // =========================================================================

    /**
     * {@code x > 5L} — long literal on rhs; analyser sets {@code binaryCaptureMask = 0}.
     * No capture emitter is invoked, so the recorded branch invocations must carry
     * no captured argument values.
     *
     * <p>Note: the analyser still derives {@code argLabels = ["x"]} for display
     * purposes (the variable name is still useful context in the popover), but the
     * bytecode emitter does not capture the actual value since {@code binaryCaptureMask = 0}.
     * This test therefore asserts on captured-values being empty, not on labels.</p>
     */
    @Test
    void cat2LongLiteral_noRuntimeCapture() throws Exception {
        ClassCoverage cc = run("cat2LongLiteral",
                new Class[]{long.class}, new Object[]{10L});

        // At runtime, no values should be captured (binaryCaptureMask = 0).
        List<AttributedInvocation> invs = branchInvocations(cc);
        assertThat(invs)
                .as("cat-2 long-literal fixture must produce at least one branch hit")
                .isNotEmpty();
        assertThat(invs).allSatisfy(inv ->
                assertThat(inv.args())
                        .as("cat-2 long-literal: no values must be captured at runtime "
                                + "(binaryCaptureMask = 0 prevents LCMP-result capture)")
                        .isEmpty());
    }

    // =========================================================================
    // Infrastructure
    // =========================================================================

    /**
     * Instruments {@link MethodCallCaptureFixtures}, loads the result in an isolated
     * classloader, invokes the named static method, and returns the {@link ClassCoverage}
     * snapshot for the fixture class.
     *
     * @param methodName the static method name
     * @param paramTypes the parameter types for the method
     * @param args       the arguments to pass at invocation
     * @return the {@link ClassCoverage} for the instrumented class
     */
    private ClassCoverage run(String methodName, Class<?>[] paramTypes, Object[] args)
            throws Exception {
        byte[] originalBytes;
        try (var stream = getClass().getClassLoader().getResourceAsStream(FIXTURE_RESOURCE)) {
            assertThat(stream).as("fixture bytes not found: %s", FIXTURE_RESOURCE).isNotNull();
            originalBytes = stream.readAllBytes();
        }

        SourceAwareInput input = new SourceAwareInput(originalBytes, classModel);
        byte[] instrumented = injector.injectProbes(FIXTURE_INTERNAL, input);

        ClassLoader isolated = new ClassLoader(getClass().getClassLoader()) {
            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                if (name.equals(FIXTURE_CLASS)) {
                    Class<?> cached = findLoadedClass(name);
                    if (cached != null) return cached;
                    return defineClass(name, instrumented, 0, instrumented.length);
                }
                return super.loadClass(name);
            }
        };

        Class<?> cls = isolated.loadClass(FIXTURE_CLASS);
        Method method = cls.getMethod(methodName, paramTypes);
        method.invoke(null, args);

        return collector.snapshot().classCoverage(FIXTURE_INTERNAL);
    }

    /**
     * Returns all {@link AttributedInvocation}s for the branch probes of {@code cc}.
     *
     * @param cc the class coverage snapshot; may be {@code null}
     * @return branch invocations, or an empty list
     */
    private static List<AttributedInvocation> branchInvocations(ClassCoverage cc) {
        if (cc == null || cc.testAttribution() == null) {
            return List.of();
        }
        Set<Integer> branchIds = branchProbeIds(cc);
        return cc.testAttribution().probeInvocations().entrySet().stream()
                .filter(e -> branchIds.contains(e.getKey()))
                .flatMap(e -> e.getValue().stream())
                .filter(inv -> !inv.testMethods().isEmpty())
                .toList();
    }

    private static Set<Integer> branchProbeIds(ClassCoverage cc) {
        if (cc == null) return Set.of();
        return cc.probeMetadata().stream()
                .filter(pm -> pm instanceof ProbeMetadata.BranchProbe)
                .map(ProbeMetadata::probeId)
                .collect(Collectors.toSet());
    }

    /**
     * Asserts that at least one branch probe on the class coverage carries the
     * specified arg labels (in order).
     *
     * @param cc            the class coverage snapshot
     * @param expectedLabels the expected label sequence
     */
    private static void assertLabels(ClassCoverage cc, String... expectedLabels) {
        if (cc == null) return;
        Set<Integer> branchIds = branchProbeIds(cc);
        boolean found = cc.probeMetadata().stream()
                .filter(pm -> branchIds.contains(pm.probeId()))
                .filter(pm -> pm instanceof ProbeMetadata.BranchProbe)
                .map(pm -> (ProbeMetadata.BranchProbe) pm)
                .anyMatch(bp -> bp.argLabels().equals(List.of(expectedLabels)));
        assertThat(found)
                .as("Expected a BranchProbe with argLabels %s but none found. "
                        + "Available labels: %s",
                        List.of(expectedLabels),
                        cc.probeMetadata().stream()
                                .filter(pm -> pm instanceof ProbeMetadata.BranchProbe)
                                .map(pm -> ((ProbeMetadata.BranchProbe) pm).argLabels())
                                .toList())
                .isTrue();
    }

    private static ProbeExecutionContext stubContext(String id) {
        return new ProbeExecutionContext() {
            @Override
            public String id() {
                return id;
            }

            @SuppressWarnings("unchecked")
            @Override
            public <T> Optional<T> get(ContextKey<T> key) {
                if (StandardContextKeys.TEST_METHOD.equals(key)) {
                    return (Optional<T>) Optional.of(id);
                }
                return Optional.empty();
            }

            @Override
            public Set<ContextKey<?>> keys() {
                return Set.of(StandardContextKeys.TEST_METHOD);
            }
        };
    }
}

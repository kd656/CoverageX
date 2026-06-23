package io.github.kd656.coveragex.core.instrument;

import io.github.kd656.coveragex.api.context.ContextKey;
import io.github.kd656.coveragex.api.context.ProbeExecutionContext;
import io.github.kd656.coveragex.api.context.StandardContextKeys;
import io.github.kd656.coveragex.api.data.AttributedInvocation;
import io.github.kd656.coveragex.api.data.ClassCoverage;
import io.github.kd656.coveragex.api.data.ClassTestCoverage;
import io.github.kd656.coveragex.api.data.ProbeMetadata;
import io.github.kd656.coveragex.api.io.internal.BinaryDataWriter;
import io.github.kd656.coveragex.core.analysis.source.impl.SourceCodeAnalyzer;
import io.github.kd656.coveragex.core.analysis.source.model.ClassModel;
import io.github.kd656.coveragex.core.analysis.source.model.SemanticIndex;
import io.github.kd656.coveragex.core.collect.CommonCoverageDataCollector;
import io.github.kd656.coveragex.core.collect.CoverageDataCollectorDelegate;
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
 * Integration tests for Phase C operand-value capture.
 *
 * <p>Each test instruments {@link BranchAttributionFixtures} with the
 * {@link SourceAwareProbeInjector}, backed by a {@link ClassModel} derived from
 * the fixture's own source file. A stub context provider attributes every branch
 * hit to the fixed test name {@code "CaptureTest#run"} so we can inspect
 * {@link ClassTestCoverage#probeInvocations()} for captured values.</p>
 *
 * <p>The source file is located via {@code getResource} on the test class
 * directory, so the path is always consistent with whatever maven put on the
 * classpath.</p>
 */
class BranchCaptureEmitterTest {

    private static final String FIXTURE_INTERNAL =
            BranchAttributionFixtures.class.getName().replace('.', '/');
    private static final String FIXTURE_RESOURCE = FIXTURE_INTERNAL + ".class";

    /**
     * Fully-qualified class name as the analyzer stores it in the {@link SemanticIndex}.
     * {@link SourceCodeAnalyzer} uses {@code getFullyQualifiedName()} from JavaParser,
     * which returns the dot-separated FQN.
     */
    private static final String FIXTURE_FQN = BranchAttributionFixtures.class.getName();

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

        // Locate the source file for BranchAttributionFixtures.java.
        // Maven places test sources under coveragex-core/src/test/java; at test
        // runtime the compiled .class files are on the classpath. We resolve the
        // source root from the .class resource location.
        URL classUrl = getClass().getClassLoader().getResource(FIXTURE_RESOURCE);
        assertThat(classUrl)
                .as("Fixture class not found on test classpath: %s", FIXTURE_RESOURCE)
                .isNotNull();

        // Walk up from target/test-classes/.../BranchAttributionFixtures.class to
        // the module root, then navigate to src/test/java.
        Path classFile = Paths.get(classUrl.toURI());
        // classFile = .../target/test-classes/io/github/.../instrument/BranchAttributionFixtures.class
        // Go up to target/test-classes, then to module root (two more ups), then to src/test/java.
        Path testClasses = classFile.getParent();
        // Walk upward until we find the "target" directory.
        Path cursor = testClasses;
        while (cursor != null && !cursor.getFileName().toString().equals("target")) {
            cursor = cursor.getParent();
        }
        assertThat(cursor)
                .as("Could not locate 'target' directory from %s", classFile)
                .isNotNull();

        Path srcRoot = cursor.getParent().resolve("src/test/java");
        assertThat(srcRoot).as("Test source root must exist: %s", srcRoot).exists();

        SemanticIndex index = new SemanticIndex();
        SourceCodeAnalyzer analyzer = new SourceCodeAnalyzer(new JavaParser(), index);
        analyzer.scan(srcRoot);

        classModel = index.getClasses().get(FIXTURE_FQN);
        assertThat(classModel)
                .as("Analyzer must produce a ClassModel for '%s' — "
                        + "check that the source file was found under %s",
                        FIXTURE_FQN, srcRoot)
                .isNotNull();
    }

    @AfterEach
    void tearDown() {
        collector.reset();
    }

    // =========================================================================
    // METHOD_CALL operand — receiver capture
    // =========================================================================

    /**
     * For {@code s.startsWith(".")}, the analyzer produces a METHOD_CALL operand
     * with {@code argLabels = ["s"]}. The capture emitter stashes the receiver
     * {@code s} before {@code INVOKEVIRTUAL startsWith} and the slot value flows
     * through the {@code Object[]} to the tracker.
     *
     * <p>Invoked with {@code s = "foo"}: startsWith returns false, so the FALSE
     * probe fires and the recorded operand value must be {@code "foo"}.</p>
     */
    @Test
    void methodCall_receiverCapturedAsOperandValue() throws Exception {
        ClassCoverage cc = runFixtureMethod(
                "methodCall", new Class[]{String.class}, new Object[]{"foo"});

        List<AttributedInvocation> invs = allBranchInvocations(cc);
        assertThat(invs)
                .as("method-call fixture must produce at least one attributed branch hit")
                .isNotEmpty();
        assertThat(invs).anySatisfy(inv ->
                assertThat(inv.args())
                        .as("captured operand value for 'methodCall(\"foo\")' must contain \"foo\"")
                        .contains("foo"));
    }

    // =========================================================================
    // BINARY_COMPARE operand — null check
    // =========================================================================

    /**
     * For {@code o == null}, the analyzer produces a BINARY_COMPARE operand with
     * {@code argLabels = ["o"]}. The capture emitter duplicates the stack reference
     * before the {@code IFNONNULL} instruction.
     *
     * <p>Invoked with {@code o = "hello"}: the null-check evaluates to false, the
     * FALSE probe fires, and the captured value must be {@code "hello"}.</p>
     */
    @Test
    void nullCheck_operandCapturedBeforeIFNONNULL() throws Exception {
        ClassCoverage cc = runFixtureMethod(
                "nullCheck", new Class[]{Object.class}, new Object[]{"hello"});

        List<AttributedInvocation> invs = allBranchInvocations(cc);
        assertThat(invs)
                .as("null-check fixture must produce at least one attributed branch hit")
                .isNotEmpty();
        assertThat(invs).anySatisfy(inv ->
                assertThat(inv.args())
                        .as("captured operand for 'nullCheck(\"hello\")' must contain \"hello\"")
                        .contains("hello"));
    }

    // =========================================================================
    // BINARY_COMPARE operand — integer comparison
    // =========================================================================

    /**
     * For {@code x > 5}, the analyzer produces a BINARY_COMPARE operand with
     * {@code argLabels = ["x"]}. The capture emitter DUPs the int operand and
     * boxes it via {@code Integer.valueOf} before the {@code IF_ICMPLE} instruction.
     *
     * <p>Invoked with {@code x = 10}: the comparison evaluates to true, the TRUE
     * probe fires, and the captured value must be {@code "10"}.</p>
     */
    @Test
    void binaryCompareInt_operandBoxedAndCaptured() throws Exception {
        ClassCoverage cc = runFixtureMethod(
                "binaryCompare", new Class[]{int.class}, new Object[]{10});

        List<AttributedInvocation> invs = allBranchInvocations(cc);
        assertThat(invs)
                .as("int-compare fixture must produce at least one attributed branch hit")
                .isNotEmpty();
        assertThat(invs).anySatisfy(inv ->
                assertThat(inv.args())
                        .as("captured operand for 'binaryCompare(10)' must contain \"10\"")
                        .contains("10"));
    }

    // =========================================================================
    // DefaultProbeInjector baseline — no capture, empty args
    // =========================================================================

    /**
     * The {@link DefaultProbeInjector} path (no source map) must still produce
     * branch hits attributed to the correct test, but with an empty operand-values
     * list — the Phase A baseline that Phase C must not break.
     */
    @Test
    void defaultInjector_branchHitAttributedWithEmptyArgs() throws Exception {
        byte[] originalBytes = readFixtureBytes();
        DefaultProbeInjector defaultInjector = new DefaultProbeInjector(collector);
        byte[] instrumented = defaultInjector.injectProbes(FIXTURE_INTERNAL, originalBytes);

        ClassLoader isolated = isolatedLoader(
                BranchAttributionFixtures.class.getName(), instrumented);
        Class<?> cls = isolated.loadClass(BranchAttributionFixtures.class.getName());
        cls.getMethod("methodCall", String.class).invoke(null, "foo");

        ClassCoverage cc = collector.snapshot().classCoverage(FIXTURE_INTERNAL);
        assertThat(cc).isNotNull();

        List<AttributedInvocation> invs = allBranchInvocations(cc);
        assertThat(invs)
                .as("default injector must attribute branch hits even without source map")
                .isNotEmpty();
        assertThat(invs).allSatisfy(inv ->
                assertThat(inv.args())
                        .as("default injector must produce empty operand-value list (no capture)")
                        .isEmpty());
    }

    // =========================================================================
    // Infrastructure
    // =========================================================================

    /**
     * Instruments {@link BranchAttributionFixtures} with the {@link SourceAwareProbeInjector}
     * backed by {@link #classModel}, loads the result in an isolated classloader, invokes
     * the named static method, and returns the {@link ClassCoverage} snapshot.
     *
     * @param methodName the name of the static method on {@link BranchAttributionFixtures}
     * @param paramTypes the method's parameter types
     * @param args       the arguments to pass to the method invocation
     * @return the {@link ClassCoverage} for {@link BranchAttributionFixtures}
     */
    private ClassCoverage runFixtureMethod(String methodName,
                                           Class<?>[] paramTypes,
                                           Object[] args) throws Exception {
        byte[] originalBytes = readFixtureBytes();
        SourceAwareInput input = new SourceAwareInput(originalBytes, classModel);
        byte[] instrumented = injector.injectProbes(FIXTURE_INTERNAL, input);

        ClassLoader isolated = isolatedLoader(
                BranchAttributionFixtures.class.getName(), instrumented);
        Class<?> cls = isolated.loadClass(BranchAttributionFixtures.class.getName());
        Method method = cls.getMethod(methodName, paramTypes);
        method.invoke(null, args);

        return collector.snapshot().classCoverage(FIXTURE_INTERNAL);
    }

    private byte[] readFixtureBytes() throws Exception {
        try (var stream = getClass().getClassLoader().getResourceAsStream(FIXTURE_RESOURCE)) {
            assertThat(stream)
                    .as("Fixture class bytes not found on classpath: %s", FIXTURE_RESOURCE)
                    .isNotNull();
            return stream.readAllBytes();
        }
    }

    /**
     * Returns every {@link AttributedInvocation} for the {@link ProbeMetadata.BranchProbe}s
     * in {@code cc}. Method-entry probes are excluded so that method-argument captures
     * (which also flow through the tracker) do not pollute the assertion.
     *
     * @param cc the class coverage snapshot; may be {@code null}
     * @return all attribution rows for branch probes in the class, or an empty list
     */
    private static List<AttributedInvocation> allBranchInvocations(ClassCoverage cc) {
        if (cc == null || cc.testAttribution() == null) {
            return List.of();
        }
        // Collect the set of branch probe ids from the metadata so we can filter.
        Set<Integer> branchProbeIds = cc.probeMetadata().stream()
                .filter(pm -> pm instanceof ProbeMetadata.BranchProbe)
                .map(ProbeMetadata::probeId)
                .collect(Collectors.toSet());

        return cc.testAttribution().probeInvocations().entrySet().stream()
                .filter(e -> branchProbeIds.contains(e.getKey()))
                .flatMap(e -> e.getValue().stream())
                .filter(inv -> !inv.testMethods().isEmpty())
                .toList();
    }

    private ClassLoader isolatedLoader(String binaryName, byte[] bytecode) {
        return new ClassLoader(getClass().getClassLoader()) {
            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                if (name.equals(binaryName)) {
                    Class<?> cached = findLoadedClass(name);
                    if (cached != null) return cached;
                    return defineClass(name, bytecode, 0, bytecode.length);
                }
                return super.loadClass(name);
            }
        };
    }

    private static ProbeExecutionContext stubContext(String id) {
        return new ProbeExecutionContext() {
            @Override public String id() { return id; }
            @SuppressWarnings("unchecked")
            @Override public <T> Optional<T> get(ContextKey<T> key) {
                if (StandardContextKeys.TEST_METHOD.equals(key)) {
                    return (Optional<T>) Optional.of(id);
                }
                return Optional.empty();
            }
            @Override public Set<ContextKey<?>> keys() {
                return Set.of(StandardContextKeys.TEST_METHOD);
            }
        };
    }
}

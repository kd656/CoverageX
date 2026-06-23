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
import io.github.kd656.coveragex.core.instrument.stubs.FlagProvider;
import io.github.kd656.coveragex.core.instrument.stubs.FlagRules;
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
 * Focused regression tests for the name+arity matching guard in
 * {@code SourceAwareProbeInjector.visitMethodInsn}.
 *
 * <p>These tests verify that when a method-call operand contains nested calls in its
 * argument list, the capture emitter fires on the <em>outer</em> call — not on an
 * inner call that happens to share the same bytecode stream. Two shapes are covered:
 * a nested call with a different name ({@code resolveUser()} inside
 * {@code canAccess(resolveUser(), role)}), and a nested boolean-returning call
 * ({@code flag()} inside {@code accepts(flag(), role)}).</p>
 */
class NestedMethodCallMatchingTest {

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
                () -> Optional.of(stubContext("NestedMatchTest#run")));
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
    // Shape 1: svc.canAccess(svc.resolveUser(), role)
    // =========================================================================

    /**
     * The inner call {@code svc.resolveUser()} visits the bytecode first. The matching
     * guard ("canAccess" != "resolveUser") prevents premature capture, so the spill
     * emitter fires on the outer {@code canAccess(user, role)} call. The captured
     * values for the TRUE branch (access granted with {@code role = "admin"}) must
     * include the role value and the user object from {@code resolveUser()}, NOT an
     * empty or partial stack left by matching {@code resolveUser()} too early.
     */
    @Test
    void nestedDifferentName_captureFiresOnOuterCall() throws Exception {
        AccessService svc = new AccessService();
        ClassCoverage cc = run("nestedDifferentName",
                new Class[]{AccessService.class, String.class},
                new Object[]{svc, "admin"});

        List<AttributedInvocation> invs = branchInvocations(cc);
        assertThat(invs)
                .as("nestedDifferentName must produce at least one attributed branch hit")
                .isNotEmpty();

        // If capture had fired on resolveUser() (wrong), the args would reflect the
        // stack at that point — before svc and role are even on the stack.
        // The outer canAccess has arity 2 (user + role); captured args must include "admin".
        assertThat(invs).anySatisfy(inv ->
                assertThat(inv.args())
                        .as("outer-call capture must include 'admin' role value")
                        .contains("admin"));
    }

    // =========================================================================
    // Shape 2: rules.accepts(provider.flag(), role)
    // =========================================================================

    /**
     * The inner call {@code provider.flag()} returns {@code boolean}. A naive
     * boolean-return guard would match it first; the name guard ("accepts" != "flag")
     * prevents this. Capture fires on the outer {@code accepts(flag, role)} call.
     *
     * <p>Test scenario: {@code provider.flag() = false}, so {@code accepts} returns
     * {@code false} and the FALSE probe fires. The captured args must include
     * {@code "ro"} (the role), confirming the outer call's stack was captured.</p>
     */
    @Test
    void nestedBooleanInner_captureFiresOnOuterCall() throws Exception {
        FlagRules rules = new FlagRules();
        FlagProvider provider = new FlagProvider(false);
        ClassCoverage cc = run("nestedBooleanInner",
                new Class[]{FlagRules.class, FlagProvider.class, String.class},
                new Object[]{rules, provider, "ro"});

        List<AttributedInvocation> invs = branchInvocations(cc);
        assertThat(invs)
                .as("nestedBooleanInner must produce at least one attributed branch hit")
                .isNotEmpty();

        // If capture had fired on provider.flag() (wrong), the args would be just
        // [provider] since flag() has arity 0. The outer accepts() has arity 2;
        // captured args must include "ro" to prove the outer call's stack was used.
        assertThat(invs).anySatisfy(inv ->
                assertThat(inv.args())
                        .as("outer-call capture must include 'ro' role value")
                        .contains("ro"));
    }

    // =========================================================================
    // Infrastructure
    // =========================================================================

    private ClassCoverage run(String methodName, Class<?>[] paramTypes, Object[] args)
            throws Exception {
        byte[] originalBytes;
        try (var stream = getClass().getClassLoader().getResourceAsStream(FIXTURE_RESOURCE)) {
            assertThat(stream).isNotNull();
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

    private static List<AttributedInvocation> branchInvocations(ClassCoverage cc) {
        if (cc == null || cc.testAttribution() == null) {
            return List.of();
        }
        Set<Integer> branchIds = cc.probeMetadata().stream()
                .filter(pm -> pm instanceof ProbeMetadata.BranchProbe)
                .map(ProbeMetadata::probeId)
                .collect(Collectors.toSet());
        return cc.testAttribution().probeInvocations().entrySet().stream()
                .filter(e -> branchIds.contains(e.getKey()))
                .flatMap(e -> e.getValue().stream())
                .filter(inv -> !inv.testMethods().isEmpty())
                .toList();
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

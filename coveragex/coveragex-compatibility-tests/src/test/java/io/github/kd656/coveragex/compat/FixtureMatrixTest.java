package io.github.kd656.coveragex.compat;

import io.github.kd656.coveragex.api.context.ContextKey;
import io.github.kd656.coveragex.api.context.ProbeExecutionContext;
import io.github.kd656.coveragex.api.context.StandardContextKeys;
import io.github.kd656.coveragex.api.data.ClassCoverage;
import io.github.kd656.coveragex.api.data.ExecutionData;
import io.github.kd656.coveragex.api.io.internal.BinaryDataWriter;
import io.github.kd656.coveragex.compat.contract.HitsContract;
import io.github.kd656.coveragex.compat.contract.PlanContract;
import io.github.kd656.coveragex.compat.spec.FixtureCatalog;
import io.github.kd656.coveragex.compat.spec.FixtureContractSpec;
import io.github.kd656.coveragex.compat.spec.FixtureContracts;
import io.github.kd656.coveragex.compat.spec.InvocationStep;
import io.github.kd656.coveragex.compat.testutil.ByteArrayClassLoader;
import io.github.kd656.coveragex.compat.testutil.BytecodeFixtures;
import io.github.kd656.coveragex.compat.testutil.PlanDumpOnFailure;
import io.github.kd656.coveragex.compat.testutil.ProbeMetadataIndex;
import io.github.kd656.coveragex.core.collect.CommonCoverageDataCollector;
import io.github.kd656.coveragex.core.collect.CoverageDataCollectorDelegate;
import io.github.kd656.coveragex.core.instrument.ClassTransformer;
import io.github.kd656.coveragex.core.probe.ProbePlan;
import io.github.kd656.coveragex.core.probe.ProbePlanBuilder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Single parameterized matrix driving every fixture in {@link FixtureCatalog}
 * through whichever contract dimensions its {@link FixtureContractSpec}
 * populates.
 *
 * <p>Each row pays the static-plan check unconditionally (cheap) and only
 * pays the instrument-and-execute cost when at least one runtime contract
 * is populated. When the spec also declares an invocation plan, the runner
 * iterates each step under a per-step context scope so the collector tags
 * hits with the test-context label for
 * {@link io.github.kd656.coveragex.compat.contract.TestAttributionContract}.</p>
 */
@ExtendWith(PlanDumpOnFailure.class)
class FixtureMatrixTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("fixtures")
    void contractsHold(String fqn, FixtureContractSpec spec) throws Exception {
        FixtureContracts contracts = spec.contracts();
        contracts.plan().ifPresent(plan -> verifyPlan(fqn, plan));
        boolean needsRuntime = contracts.hits().isPresent()
                || contracts.args().isPresent()
                || contracts.invocations().isPresent()
                || contracts.attribution().isPresent();
        if (needsRuntime) {
            verifyRuntime(fqn, spec);
        }
    }

    private static void verifyPlan(String fqn, PlanContract contract) {
        byte[] bytes = BytecodeFixtures.load(fqn);
        ProbePlan plan = new ProbePlanBuilder().build(fqn.replace('.', '/'), bytes, null);
        PlanDumpOnFailure.capture(fqn, plan);
        contract.verify(plan);
    }

    private void verifyRuntime(String fqn, FixtureContractSpec spec) throws Exception {
        FixtureContracts contracts = spec.contracts();
        var collector = new CommonCoverageDataCollector(
                new BinaryDataWriter(),
                () -> CoverageDataCollectorDelegate.contextRegistry().current());

        try (var ignored = CoverageDataCollectorDelegate.registry().scope(collector)) {
            Map<String, byte[]> rawClasses = BytecodeFixtures.loadWithNestedClasses(fqn);
            String classId = fqn.replace('.', '/');
            byte[] mainRaw = rawClasses.get(classId);

            ClassTransformer transformer = new ClassTransformer(
                    collector,
                    List.of(fqn.substring(0, fqn.lastIndexOf('.')) + ".**"),
                    List.of(),
                    null);

            Map<String, byte[]> instrumented = new LinkedHashMap<>();
            for (var entry : rawClasses.entrySet()) {
                byte[] out = transformer.transform(
                        getClass().getClassLoader(), entry.getKey(), null, null, entry.getValue());
                instrumented.put(entry.getKey(), out != null ? out : entry.getValue());
            }

            Class<?> cls = new ByteArrayClassLoader(instrumented).loadClass(fqn);
            cls.getMethod("execute").invoke(null);

            // Run any invocation-plan steps under their declared context.
            // Hits/args/invocations from execute() above + the plan are
            // additive; attribution is recorded only for the plan steps (the
            // execute() call ran with no context installed).
            for (InvocationStep step : spec.invocationPlan()) {
                try (var ignored1 = CoverageDataCollectorDelegate.contextRegistry()
                        .scope(() -> Optional.of(stubContext(step.testContext())))) {
                    invokeStep(cls, step);
                }
            }

            if (contracts.hits().isPresent()) {
                ExecutionData hitSnapshot = collector.snapshot();
                ClassCoverage hitCc = hitSnapshot.classes().get(classId);
                Map<Integer, Integer> hitCounts = new LinkedHashMap<>();
                hitCc.hits().forEach((id, h) -> hitCounts.put(id, h.count()));
                contracts.hits().get().verify(hitCounts, ProbeMetadataIndex.from(mainRaw));
            }

            if (contracts.args().isPresent()
                    || contracts.invocations().isPresent()
                    || contracts.attribution().isPresent()) {
                ExecutionData snapshot = collector.snapshot();
                ClassCoverage cc = snapshot.classes().get(classId);
                contracts.args().ifPresent(c -> c.verify(cc));
                contracts.invocations().ifPresent(c -> c.verify(cc));
                contracts.attribution().ifPresent(c -> c.verify(cc));
            }
        }
    }

    private static void invokeStep(Class<?> cls, InvocationStep step) throws Exception {
        Method method = findMethod(cls, step.methodName(), step.args().size());
        method.invoke(null, step.args().toArray());
    }

    private static Method findMethod(Class<?> cls, String name, int paramCount) {
        for (Method m : cls.getDeclaredMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == paramCount) {
                m.setAccessible(true);
                return m;
            }
        }
        throw new IllegalStateException(
                "No method named '" + name + "' with " + paramCount
                        + " parameter(s) on " + cls.getName());
    }

    private static ProbeExecutionContext stubContext(String label) {
        return new ProbeExecutionContext() {
            @Override public String id() { return label; }
            @SuppressWarnings("unchecked")
            @Override public <T> Optional<T> get(ContextKey<T> key) {
                if (StandardContextKeys.TEST_METHOD.equals(key)) {
                    return (Optional<T>) Optional.of(label);
                }
                return Optional.empty();
            }
            @Override public Set<ContextKey<?>> keys() {
                return Set.of(StandardContextKeys.TEST_METHOD);
            }
        };
    }

    static Stream<Arguments> fixtures() {
        return FixtureCatalog.all().stream()
                .map(spec -> Arguments.of(spec.fqn(), spec));
    }
}

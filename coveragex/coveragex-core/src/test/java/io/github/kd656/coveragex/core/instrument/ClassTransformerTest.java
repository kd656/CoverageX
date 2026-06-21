package io.github.kd656.coveragex.core.instrument;

import io.github.kd656.coveragex.core.collect.CoverageDataCollectorDelegate;
import io.github.kd656.coveragex.core.collect.CommonCoverageDataCollector;
import io.github.kd656.coveragex.core.instrument.ClassTransformer;
import io.github.kd656.coveragex.api.data.ClassCoverage;
import io.github.kd656.coveragex.api.data.ExecutionData;
import io.github.kd656.coveragex.api.io.internal.BinaryDataWriter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClassTransformerTest {

    /**
     * Simple fixture class whose bytecode we instrument in tests.
     * Must be public so the transformed class can be instantiated via reflection.
     */
    public static class SimpleFixture {
        public void doWork() {
            // intentionally empty — we only care that the method entry probe fires
        }
    }

    private CommonCoverageDataCollector collector;

    @BeforeEach
    void setUp() {
        collector = new CommonCoverageDataCollector(new BinaryDataWriter());
        collector.reset();
        CoverageDataCollectorDelegate.registry().installGlobal(collector);
    }

    @AfterEach
    void tearDown() {
        collector.reset();
    }

    @Test
    void transformedClassRecordsProbeHitOnMethodCall() throws Exception {
        // Internal class name in JVM slash notation
        String fixtureInternalName = SimpleFixture.class.getName().replace('.', '/');

        // Load the original bytecode from the classpath
        byte[] originalBytes;
        String resourcePath = fixtureInternalName + ".class";
        try (var stream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            assertThat(stream)
                .as("Fixture class resource not found on classpath: %s", resourcePath)
                .isNotNull();
            originalBytes = stream.readAllBytes();
        }

        // Create a transformer that includes the package containing our fixture.
        // Providing a non-empty includes list prevents the constructor from adding
        // the default "io.github.kd656.coveragex.**" exclude, which would otherwise block our fixture.
        // Note: we use a package wildcard rather than the exact class name because
        // the dollar sign in inner-class names (e.g. "Outer$Inner") is a special
        // character in regex and would break the glob-to-regex conversion in matchesPattern().
        ClassTransformer transformer = new ClassTransformer(
            collector,
            List.of("io.github.kd656.coveragex.core.instrument.**"),
            List.of(),
            null  // no coverage map; uses DefaultProbeInjector fallback
        );

        // Transform the bytecode (null protectionDomain → not treated as test output)
        byte[] transformedBytes = transformer.transform(
            getClass().getClassLoader(),
            fixtureInternalName,
            null,
            null,
            originalBytes
        );

        assertThat(transformedBytes)
            .as("Transformer must return instrumented bytes for an included class")
            .isNotNull();

        // Load the transformed class, bypassing parent delegation for this one class
        // so we get our instrumented version rather than the already-loaded original.
        Class<?> instrumentedClass = loadClass(SimpleFixture.class, transformedBytes);
        Object instance = instrumentedClass.getDeclaredConstructor().newInstance();
        Method doWork = instrumentedClass.getDeclaredMethod("doWork");
        doWork.invoke(instance);

        // ProbeInjector registers the class with the collector during transformation;
        // after invoking the method the entry probe must be marked as hit.
        boolean[] probeData = collector.getProbeData(fixtureInternalName);
        assertThat(probeData)
            .as("ProbeInjector must register the class with the collector during transformation")
            .isNotNull();

        boolean anyProbeHit = false;
        for (boolean hit : probeData) {
            if (hit) { anyProbeHit = true; break; }
        }
        assertThat(anyProbeHit)
            .as("At least one probe must be recorded after calling doWork()")
            .isTrue();

        // Additionally verify that the snapshot captures the method invocation
        ExecutionData snapshot = collector.snapshot();
        ClassCoverage classCoverage = snapshot.classCoverage(fixtureInternalName);
        assertThat(classCoverage).isNotNull();
        assertThat(classCoverage.methodHits())
            .as("Entry probe for doWork must be recorded in the snapshot")
            .isNotEmpty();
    }

    private Class<?> loadClass(Class<?> clazz, byte[] transformedBytes) throws ClassNotFoundException {
        ClassLoader loader = new ClassLoader(getClass().getClassLoader()) {
            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                if (name.equals(SimpleFixture.class.getName())) {
                    Class<?> cached = findLoadedClass(name);
                    if (cached != null) return cached;
                    return defineClass(name, transformedBytes, 0, transformedBytes.length);
                }
                return super.loadClass(name);
            }
        };

        // Invoke doWork() on an instance of the instrumented class
        Class<?> instrumentedClass = loader.loadClass(clazz.getName());
        return instrumentedClass;
    }
}

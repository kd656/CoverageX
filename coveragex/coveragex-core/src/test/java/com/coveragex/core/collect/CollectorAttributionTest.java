package com.coveragex.core.collect;

import com.coveragex.api.context.ContextKey;
import com.coveragex.api.context.ProbeExecutionContext;
import com.coveragex.api.context.StandardContextKeys;
import com.coveragex.api.data.ClassCoverage;
import com.coveragex.api.data.ClassTestCoverage;
import com.coveragex.api.data.ExecutionData;
import com.coveragex.api.data.ProbeMetadata;
import com.coveragex.api.data.TestTrackingSnapshot;
import com.coveragex.api.io.internal.BinaryDataWriter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that a collector receives a context provider via DI,
 * attributes every hit through its own tracker, and {@link
 * CommonCoverageDataCollector#snapshot()} stitches the attribution into each
 * {@link ClassCoverage} so the serialized {@code .exec} carries test names
 * end-to-end.
 */
class CollectorAttributionTest {

    @Test
    void snapshotCarriesEmptyAttributionWhenProviderYieldsNothing() {
        var collector = new CommonCoverageDataCollector(new BinaryDataWriter());
        collector.registerClass("com/example/Foo", 1, metadata(1));
        collector.recordHit("com/example/Foo", "doX", 0, null);

        assertThat(collector.snapshotTracker()).isEqualTo(TestTrackingSnapshot.empty());
        ClassCoverage cc = collector.snapshot().classes().get("com/example/Foo");
        assertThat(cc.testAttribution()).isEqualTo(ClassTestCoverage.empty("com/example/Foo"));
    }

    @Test
    void snapshotStitchesAttributionPerClassWhenProviderYieldsContext() {
        var contextRef = new AtomicReference<>(stubContext("MyTest#alpha"));
        var collector = new CommonCoverageDataCollector(
                new BinaryDataWriter(),
                () -> Optional.ofNullable(contextRef.get()));

        collector.registerClass("com/example/Bar", 1, metadata(1));
        collector.recordHit("com/example/Bar", "doY", 0, new Object[]{1});

        ExecutionData exec = collector.snapshot();
        ClassCoverage cc = exec.classes().get("com/example/Bar");

        assertThat(cc.testAttribution().probeInvocations()).containsKey(0);
        assertThat(cc.testAttribution().probeInvocations().get(0))
                .anySatisfy(ai -> assertThat(ai.testMethods()).contains("MyTest#alpha"));
    }

    @Test
    void resetClearsAttribution() {
        var contextRef = new AtomicReference<>(stubContext("MyTest#alpha"));
        var collector = new CommonCoverageDataCollector(
                new BinaryDataWriter(),
                () -> Optional.ofNullable(contextRef.get()));
        collector.registerClass("com/example/Baz", 1, metadata(1));
        collector.recordHit("com/example/Baz", "m", 0, null);

        collector.reset();

        collector.registerClass("com/example/Baz", 1, metadata(1));
        ClassCoverage cc = collector.snapshot().classes().get("com/example/Baz");
        assertThat(cc.testAttribution()).isEqualTo(ClassTestCoverage.empty("com/example/Baz"));
    }

    @Test
    void productionWiringReadsThroughContextRegistry() {
        var collector = new CommonCoverageDataCollector(
                new BinaryDataWriter(),
                () -> CoverageDataCollectorDelegate.contextRegistry().current());
        collector.registerClass("com/example/Qux", 1, metadata(1));

        try (var scope = CoverageDataCollectorDelegate.contextRegistry()
                .scope(() -> Optional.of(stubContext("Wired#test")))) {
            collector.recordHit("com/example/Qux", "m", 0, null);
        }

        ClassCoverage cc = collector.snapshot().classes().get("com/example/Qux");
        assertThat(cc.testAttribution().probeInvocations()).containsKey(0);
        assertThat(cc.testAttribution().probeInvocations().get(0))
                .anySatisfy(ai -> assertThat(ai.testMethods()).contains("Wired#test"));
    }

    private List<ProbeMetadata> metadata(int count) {
        return java.util.stream.IntStream.range(0, count)
                .<ProbeMetadata>mapToObj(i -> new ProbeMetadata.MethodProbe(i, "m" + i, 0, 0))
                .toList();
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

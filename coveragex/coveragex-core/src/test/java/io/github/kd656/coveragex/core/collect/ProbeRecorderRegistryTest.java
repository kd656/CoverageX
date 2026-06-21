package io.github.kd656.coveragex.core.collect;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class ProbeRecorderRegistryTest {

    @Test
    void currentReturnsNoOpByDefault() {
        var registry = new ProbeRecorderRegistry();

        assertThat(registry.current()).isSameAs(NoOpProbeRecorder.INSTANCE);
        assertThat(registry.globalRecorder()).isSameAs(NoOpProbeRecorder.INSTANCE);
    }

    @Test
    void installGlobalReplacesTheNoOpRecorder() {
        var registry = new ProbeRecorderRegistry();
        var captured = new CapturingRecorder();

        registry.installGlobal(captured);

        registry.current().recordHit("ClassA", "doX", 0, new Object[]{1});

        assertThat(captured.hits).hasSize(1);
        assertThat(captured.hits.get(0).classId()).isEqualTo("ClassA");
    }

    @Test
    void scopedRecorderTakesPrecedenceOverGlobalOnSameThread() {
        var registry = new ProbeRecorderRegistry();
        var global = new CapturingRecorder();
        var scoped = new CapturingRecorder();
        registry.installGlobal(global);

        try (var scope = registry.scope(scoped)) {
            registry.current().recordHit("ClassA", "doX", 0, null);
        }

        assertThat(scoped.hits).hasSize(1);
        assertThat(global.hits).isEmpty();
    }

    @Test
    void closingScopeRestoresGlobalRouting() {
        var registry = new ProbeRecorderRegistry();
        var global = new CapturingRecorder();
        var scoped = new CapturingRecorder();
        registry.installGlobal(global);

        try (var scope = registry.scope(scoped)) {
            registry.current().recordHit("Inside", "m", 0, null);
        }
        registry.current().recordHit("After", "m", 0, null);

        assertThat(scoped.hits).extracting(Hit::classId).containsExactly("Inside");
        assertThat(global.hits).extracting(Hit::classId).containsExactly("After");
    }

    @Test
    void nestedScopesRestoreOuterRecorderNotGlobal() {
        var registry = new ProbeRecorderRegistry();
        var global = new CapturingRecorder();
        var outer = new CapturingRecorder();
        var inner = new CapturingRecorder();
        registry.installGlobal(global);

        try (var outerScope = registry.scope(outer)) {
            registry.current().recordHit("OuterBefore", "m", 0, null);
            try (var innerScope = registry.scope(inner)) {
                registry.current().recordHit("Inner", "m", 0, null);
            }
            registry.current().recordHit("OuterAfter", "m", 0, null);
        }
        registry.current().recordHit("AfterAll", "m", 0, null);

        assertThat(outer.hits).extracting(Hit::classId).containsExactly("OuterBefore", "OuterAfter");
        assertThat(inner.hits).extracting(Hit::classId).containsExactly("Inner");
        assertThat(global.hits).extracting(Hit::classId).containsExactly("AfterAll");
    }

    @Test
    void scopeOnOneThreadDoesNotAffectOtherThreads() throws Exception {
        var registry = new ProbeRecorderRegistry();
        var global = new CapturingRecorder();
        var scoped = new CapturingRecorder();
        registry.installGlobal(global);

        var workerInsideScope = new CountDownLatch(1);
        var mainHitRecorded = new CountDownLatch(1);
        var workerSawRecorder = new AtomicReference<ProbeRecorder>();

        Thread worker = new Thread(() -> {
            try (var scope = registry.scope(scoped)) {
                workerInsideScope.countDown();
                mainHitRecorded.await();
                workerSawRecorder.set(registry.current());
                registry.current().recordHit("Worker", "m", 0, null);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "registry-test-worker");
        worker.start();

        workerInsideScope.await();
        registry.current().recordHit("Main", "m", 0, null);
        mainHitRecorded.countDown();
        worker.join();

        assertThat(workerSawRecorder.get()).isSameAs(scoped);
        assertThat(scoped.hits).extracting(Hit::classId).containsExactly("Worker");
        assertThat(global.hits).extracting(Hit::classId).containsExactly("Main");
    }

    @Test
    void doubleCloseIsNoOp() {
        var registry = new ProbeRecorderRegistry();
        var global = new CapturingRecorder();
        var scoped = new CapturingRecorder();
        registry.installGlobal(global);

        var scope = registry.scope(scoped);
        scope.close();
        scope.close();

        registry.current().recordHit("After", "m", 0, null);
        assertThat(global.hits).extracting(Hit::classId).containsExactly("After");
        assertThat(scoped.hits).isEmpty();
    }

    private record Hit(String classId, String methodName, int probeId) {}

    private static final class CapturingRecorder implements ProbeRecorder {
        final List<Hit> hits = new ArrayList<>();

        @Override
        public void recordHit(String classId, String methodName, int probeId, Object[] args) {
            hits.add(new Hit(classId, methodName, probeId));
        }
    }
}

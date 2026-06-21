package com.coveragex.core.collect;

import com.coveragex.api.data.ExecutionData;
import com.coveragex.api.data.ClassCoverage;
import com.coveragex.api.data.MethodHit;
import com.coveragex.api.data.ProbeHit;
import com.coveragex.api.data.ProbeMetadata;
import com.coveragex.api.io.CoverageDataReader;
import com.coveragex.api.io.CoverageDataWriter;
import com.coveragex.api.io.internal.BinaryDataReader;
import com.coveragex.api.io.internal.BinaryDataWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class CoverageDataCollectorTest {

    private final CoverageDataWriter<ExecutionData> coverageDataWriter = new BinaryDataWriter();
    private final CoverageDataReader<ExecutionData> coverageDataReader = new BinaryDataReader();

    private final CommonCoverageDataCollector collector = new CommonCoverageDataCollector(coverageDataWriter);

    @BeforeEach
    void setUp() {
        collector.reset();
    }

    @Test
    void singletoncollectorIsConsistent() {
        assertThat(collector).isSameAs(collector);
    }

    @Test
    void registerClassCreatesProbeArray() {
        boolean[] probes = collector.registerClass("com/example/Foo", 5, metadata(5));

        assertThat(probes).hasSize(5);
        assertThat(probes).containsOnly(false);
    }

    @Test
    void recordHitMarksProbe() {
        collector.registerClass("com/example/Bar", 3, metadata(3));

        collector.recordHit("com/example/Bar", "doWork", 1, null);

        boolean[] probes = collector.getProbeData("com/example/Bar");
        assertThat(probes[0]).isFalse();
        assertThat(probes[1]).isTrue();
        assertThat(probes[2]).isFalse();
    }

    @Test
    void recordHitIgnoresUnregisteredClass() {
        collector.recordHit("com/example/Unknown", "someMethod", 0, null);
    }

    @Test
    void recordHitIgnoresOutOfBoundsProbe() {
        collector.registerClass("com/example/Baz", 2, metadata(2));

        collector.recordHit("com/example/Baz", "run", 5, null);
        collector.recordHit("com/example/Baz", "run", -1, null);

        boolean[] probes = collector.getProbeData("com/example/Baz");
        assertThat(probes).containsExactly(false, false);
    }

    @Test
    void resetClearsAllData() {
        collector.registerClass("com/example/A", 3, metadata(3));
        collector.registerClass("com/example/B", 2, metadata(2));

        collector.reset();

        assertThat(collector.classCount()).isZero();
        assertThat(collector.getProbeData("com/example/A")).isNull();
    }

    @Test
    void recordHitWithMethodNameAndArgs_storesData() {
        collector.registerClass("com/example/Svc", 3, metadata(3));

        Object[] args = new Object[]{"hello", 42};
        collector.recordHit("com/example/Svc", "process", 0, args);

        boolean[] probes = collector.getProbeData("com/example/Svc");
        assertThat(probes[0]).isTrue();

        ExecutionData snapshot = collector.snapshot();
        ClassCoverage cc = snapshot.classCoverage("com/example/Svc");
        assertThat(cc).isNotNull();
        assertThat(cc.methodHits()).containsKey(0);
        assertThat(cc.probeMetadata()).hasSize(3);

        MethodHit hit = cc.methodHits().get(0);
        assertThat(hit.methodName()).isEqualTo("process");
        assertThat(hit.invocations()).hasSize(1);
        assertThat(hit.invocations().get(0).args()).containsExactly("hello", "42");
        assertThat(hit.invocations().get(0).count()).isEqualTo(1);
    }

    @Test
    void recordHitWithNullArgs_stillRecordsHit() {
        collector.registerClass("com/example/Svc2", 5, metadata(5));

        collector.recordHit("com/example/Svc2", "compute", 2, null);

        boolean[] probes = collector.getProbeData("com/example/Svc2");
        assertThat(probes[2]).isTrue();

        ExecutionData snapshot = collector.snapshot();
        ClassCoverage cc = snapshot.classCoverage("com/example/Svc2");
        assertThat(cc).isNotNull();
        assertThat(cc.methodHits()).isEmpty();
        assertThat(cc.probeMetadata()).hasSize(5);
    }

    @Test
    void snapshotReturnsEmptyForUnregisteredClass() {
        ExecutionData snapshot = collector.snapshot();
        assertThat(snapshot.classCoverage("com/example/NeverSeen")).isNull();
    }

    @Test
    void recordHitSameMethodMultipleTimes_countsAggregated() {
        collector.registerClass("com/example/Counter", 1, metadata(1));

        Object[] args1 = new Object[]{"alpha"};
        collector.recordHit("com/example/Counter", "run", 0, args1);
        collector.recordHit("com/example/Counter", "run", 0, args1);
        collector.recordHit("com/example/Counter", "run", 0, args1);

        Object[] args2 = new Object[]{"beta"};
        collector.recordHit("com/example/Counter", "run", 0, args2);
        collector.recordHit("com/example/Counter", "run", 0, args2);

        ExecutionData snapshot = collector.snapshot();
        ClassCoverage cc = snapshot.classCoverage("com/example/Counter");
        assertThat(cc).isNotNull();
        assertThat(cc.methodHits()).containsKey(0);
        assertThat(cc.probeMetadata()).hasSize(1);

        MethodHit hit = cc.methodHits().get(0);
        assertThat(hit.methodName()).isEqualTo("run");
        assertThat(hit.invocations()).hasSize(2);

        boolean foundAlpha = false;
        boolean foundBeta = false;
        for (com.coveragex.api.data.InvocationRecord rec : hit.invocations()) {
            if (rec.args().equals(List.of("alpha"))) {
                assertThat(rec.count()).isEqualTo(3);
                foundAlpha = true;
            } else if (rec.args().equals(List.of("beta"))) {
                assertThat(rec.count()).isEqualTo(2);
                foundBeta = true;
            }
        }

        assertThat(foundAlpha).as("InvocationRecord for 'alpha' not found").isTrue();
        assertThat(foundBeta).as("InvocationRecord for 'beta' not found").isTrue();
    }

    @Test
    void flushWritesMethodNamesAndArgs(@TempDir Path tempDir) throws Exception {
        Path outputFile = tempDir.resolve("coveragex.exec");

        String originalDestFile = System.getProperty("coveragex.destFile");
        System.setProperty("coveragex.destFile", outputFile.toAbsolutePath().toString());

        try {
            collector.registerClass("com/example/Service", 3, metadata(3));
            Object[] args = new Object[]{"order-99", 42};
            collector.recordHit("com/example/Service", "processOrder", 0, args);

            collector.flush();
        } finally {
            if (originalDestFile == null) {
                System.clearProperty("coveragex.destFile");
            } else {
                System.setProperty("coveragex.destFile", originalDestFile);
            }
        }

        assertThat(Files.exists(outputFile)).isTrue();

        ExecutionData data = coverageDataReader.read(outputFile);

        assertThat(data.classCount()).isEqualTo(1);

        ClassCoverage cc = data.classes().get("com/example/Service");
        assertThat(cc).isNotNull();
        assertThat(cc.probeHits()).hasSize(3);
        assertThat(cc.probeHits()[0]).isTrue();
        assertThat(cc.probeMetadata()).hasSize(3);

        Map<Integer, MethodHit> classHits = cc.methodHits();
        assertThat(classHits).isNotNull().hasSize(1);

        MethodHit hit = classHits.get(0);
        assertThat(hit.methodName()).isEqualTo("processOrder");
        assertThat(hit.invocations()).hasSize(1);
        assertThat(hit.invocations().getFirst().args()).containsExactly("order-99", "42");
        assertThat(hit.invocations().getFirst().count()).isEqualTo(1);
    }

    @Test
    void registerSameClassTwice_sameMetadata_isIdempotent() {
        List<ProbeMetadata> meta = metadata(3);

        boolean[] first = collector.registerClass("com/example/Dup", 3, meta);
        boolean[] second = collector.registerClass("com/example/Dup", 3, meta);

        assertThat(second).isSameAs(first);
        assertThat(collector.classCount()).isEqualTo(1);
    }

    @Test
    void snapshotProbeHitsArrayIsDefensiveCopy() {
        collector.registerClass("com/example/Def", 3, metadata(3));
        collector.recordHit("com/example/Def", "go", 1, null);

        boolean[] snapshotHits = collector.snapshot().classCoverage("com/example/Def").probeHits();
        snapshotHits[1] = false;
        snapshotHits[2] = true;

        boolean[] internal = collector.getProbeData("com/example/Def");
        assertThat(internal[1]).isTrue();
        assertThat(internal[2]).isFalse();
    }

    @Test
    void concurrentHitsSameProbe_probeRemainsTrue() throws InterruptedException {
        collector.registerClass("com/example/Conc", 1, metadata(1));

        Thread[] threads = new Thread[20];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> collector.recordHit("com/example/Conc", "m", 0, null));
        }
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        assertThat(collector.getProbeData("com/example/Conc")[0]).isTrue();
    }

    @Test
    void concurrentClassRegistration_allClassesRegistered() throws InterruptedException {
        int count = 10;
        Thread[] threads = new Thread[count];
        for (int i = 0; i < count; i++) {
            final int idx = i;
            threads[i] = new Thread(() ->
                collector.registerClass("com/example/Parallel" + idx, 1, metadata(1)));
        }
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        assertThat(collector.classCount()).isEqualTo(count);
    }

    @Test
    void flushWithNoClasses_noExceptionReturnsEarly() {
        assertThatCode(collector::flush).doesNotThrowAnyException();
    }

    @Test
    void flushToMissingParentDirectory_parentDirectoriesCreated(@TempDir Path tempDir) throws Exception {
        Path nested = tempDir.resolve("a/b/c/coveragex.exec");
        String orig = System.getProperty("coveragex.destFile");
        System.setProperty("coveragex.destFile", nested.toAbsolutePath().toString());
        try {
            collector.registerClass("com/example/Dir", 1, metadata(1));
            collector.recordHit("com/example/Dir", "run", 0, null);
            collector.flush();
            assertThat(Files.exists(nested)).isTrue();
        } finally {
            if (orig == null) System.clearProperty("coveragex.destFile");
            else System.setProperty("coveragex.destFile", orig);
        }
    }

    @Test
    void flushWhenWriterThrows_logsErrorWithoutPropagating() {
        CoverageDataWriter<ExecutionData> throwing = new CoverageDataWriter<ExecutionData>() {
            @Override public void write(java.nio.file.Path p, ExecutionData d) throws IOException {
                throw new IOException("simulated");
            }
        };
        CommonCoverageDataCollector local = new CommonCoverageDataCollector(throwing);
        local.registerClass("com/example/Err", 1, metadata(1));
        local.recordHit("com/example/Err", "m", 0, null);

        assertThatCode(() -> local.flush()).doesNotThrowAnyException();
    }

    private static List<ProbeMetadata> metadata(int probeCount) {
        return java.util.stream.IntStream.range(0, probeCount)
                .mapToObj(CoverageDataCollectorTest::probeMetadata)
                .toList();
    }

    @Test
    void hitsMapCountsEveryRecordHitCall_branchProbe() {
        collector.registerClass("com/example/Loop", 1, metadata(1));

        for (int i = 0; i < 7; i++) {
            collector.recordHit("com/example/Loop", "loopBody", 0, null);
        }

        ExecutionData snapshot = collector.snapshot();
        ClassCoverage cc = snapshot.classCoverage("com/example/Loop");
        assertThat(cc.hits()).containsKey(0);
        assertThat(cc.hits().get(0).count()).isEqualTo(7);
    }

    @Test
    void hitsMapAccumulatesConcurrently() throws Exception {
        int threads = 8;
        int hitsPerThread = 50_000;
        collector.registerClass("com/example/Concurrent", 1, metadata(1));

        CyclicBarrier startGate = new CyclicBarrier(threads);
        try (ExecutorService pool = Executors.newFixedThreadPool(threads)){
            List<Future<?>> futures = new ArrayList<>(threads);
            for (int t = 0; t < threads; t++) {
                futures.add(pool.submit(() -> {
                    startGate.await();
                    for (int i = 0; i < hitsPerThread; i++) {
                        collector.recordHit("com/example/Concurrent", "hot", 0, null);
                    }
                    return null;
                }));
            }
            for (Future<?> f : futures) {
                f.get(30, TimeUnit.SECONDS);
            }
            ExecutionData snapshot = collector.snapshot();
            ProbeHit hit = snapshot.classCoverage("com/example/Concurrent").hits().get(0);
            assertThat(hit.count()).isEqualTo(threads * hitsPerThread);
        }
    }

    private static ProbeMetadata probeMetadata(int probeId) {
        return new ProbeMetadata.MethodProbe(
                probeId,
                "method" + probeId,
                -1,
                -1
        );
    }
}
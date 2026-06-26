package io.github.kd656.coveragex.core.collect;

import io.github.kd656.coveragex.api.context.ProbeExecutionContext;
import io.github.kd656.coveragex.api.data.ClassCoverage;
import io.github.kd656.coveragex.api.data.ClassTestCoverage;
import io.github.kd656.coveragex.api.data.ExecutionData;
import io.github.kd656.coveragex.api.data.InvocationRecord;
import io.github.kd656.coveragex.api.data.MethodHit;
import io.github.kd656.coveragex.api.data.ProbeHit;
import io.github.kd656.coveragex.api.data.ProbeMetadata;
import io.github.kd656.coveragex.api.data.TestTrackingSnapshot;
import io.github.kd656.coveragex.api.io.CoverageDataWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.function.Supplier;

/**
 * Collects runtime coverage probe hits and method-entry invocation frequencies.
 *
 * <p>This collector is designed for low-overhead concurrent writes from injected probe callbacks.
 * Probe-hit recording is idempotent: probes only transition from {@code false} to {@code true}.</p>
 *
 * <p>For method-entry probes, argument combinations are aggregated and counted so the snapshot
 * contains both structural coverage and observed invocation frequencies.</p>
 */
public final class CommonCoverageDataCollector implements CoverageDataCollector {

    private static final Logger LOG = LoggerFactory.getLogger(CommonCoverageDataCollector.class);
    private static final String DEFAULT_DEST_FILE = "target/coveragex.exec";
    private static final String DEST_FILE_PROPERTY = "coveragex.destFile";

    private final CoverageDataWriter<ExecutionData> coverageDataWriter;
    private final Supplier<Optional<ProbeExecutionContext>> contextProvider;

    /**
     * Holds all per-class coverage state in one place.
     */
    private final ConcurrentMap<String, ClassCoverageState> classesById = new ConcurrentHashMap<>();

    /**
     * Test-attribution tracker. Populated on every {@link #recordHit recordHit}
     * when {@link #contextProvider} yields a context; otherwise stays empty.
     * Owned per-collector so multiple collectors (production global, test
     * scopes) don't share attribution.
     */
    private final CommonTestProbeTracker tracker = new CommonTestProbeTracker();

    /**
     * Convenience constructor for callers that don't need test attribution
     * (most unit tests, the compat-test matrix). Uses a no-context provider,
     * so the tracker stays empty and {@link #snapshot()} produces
     * {@code ClassCoverage} entries with {@code ClassTestCoverage.empty(...)}.
     */
    public CommonCoverageDataCollector(CoverageDataWriter<ExecutionData> coverageDataWriter) {
        this(coverageDataWriter, Optional::empty);
    }

    /**
     * Primary constructor. The {@code contextProvider} is consulted on every
     * recorded hit; whenever it returns a non-empty context, the hit is
     * attributed to that context in the tracker. Production wires this to
     * {@code () -> CoverageDataCollectorDelegate.contextRegistry().current()}
     * so the recorder reads through an abstraction rather than reaching into
     * static state directly.
     */
    public CommonCoverageDataCollector(
            CoverageDataWriter<ExecutionData> coverageDataWriter,
            Supplier<Optional<ProbeExecutionContext>> contextProvider) {
        this.coverageDataWriter = Objects.requireNonNull(coverageDataWriter, "coverageDataWriter must not be null");
        this.contextProvider = Objects.requireNonNull(contextProvider, "contextProvider must not be null");
    }

    public boolean[] registerClass(String classId, int probeCount, List<ProbeMetadata> metadata) {
        validateRegistration(classId, probeCount, metadata);

        ClassCoverageState state = classesById.compute(classId, (id, existing) -> {
            if (existing == null) {
                return new ClassCoverageState(probeCount, metadata);
            }

            validateConsistentRegistration(classId, probeCount, metadata, existing);
            return existing;
        });

        return state.probeHits();
    }

    /**
     * Records a method-entry probe hit together with the captured argument
     * values. Only method-entry hits contribute to the per-method invocation
     * report.
     *
     * @param classId    the internal class name
     * @param methodName the simple method name (e.g. {@code doSomething})
     * @param probeId    the probe index within the class
     * @param args       the boxed argument values, never {@code null}
     */
    public void recordMethodEntry(String classId, String methodName,
                                   int probeId, Object[] args) {
        markHit(classId, probeId);
        recordInvocationEntry(classId, methodName, probeId, args);
        attributeToTest(classId, probeId, args);
    }

    /**
     * Records a branch-direction probe hit together with any operand values
     * that the capture emitter stashed (may be an empty array when no source
     * map was available at instrumentation time).
     *
     * @param classId       the internal class name
     * @param probeId       the probe index within the class
     * @param operandValues the captured operand values, never {@code null}
     */
    public void recordBranchHit(String classId, int probeId, Object[] operandValues) {
        markHit(classId, probeId);
        attributeToTest(classId, probeId, operandValues);
    }

    /**
     * Records a probe hit that carries no payload — return, throw, or segment
     * probes.
     *
     * @param classId the internal class name
     * @param probeId the probe index within the class
     */
    public void recordSimpleHit(String classId, int probeId) {
        markHit(classId, probeId);
    }

    /**
     * Marks the probe as hit and increments its counter. Shared preamble for
     * every kind of probe.
     */
    private void markHit(String classId, int probeId) {
        markProbeAsHit(classId, probeId);
        incrementProbeCounter(classId, probeId);
    }

    /**
     * Attributes the probe hit to the calling thread's test-execution context,
     * when one is available. The supplied payload becomes the dedup key for
     * the attribution row (alongside {@code probeId}).
     */
    private void attributeToTest(String classId, int probeId, Object[] payload) {
        contextProvider.get().ifPresent(ctx ->
                tracker.record(classId, probeId, toNullableStrings(payload), ctx));
    }

    private void incrementProbeCounter(String classId, int probeId) {
        ClassCoverageState state = classesById.get(classId);
        if (state == null) {
            return;
        }

        AtomicIntegerArray counters = state.counters();
        if (probeId < 0 || probeId >= counters.length()) {
            return;
        }

        counters.incrementAndGet(probeId);
    }

    public boolean[] getProbeData(String classId) {
        ClassCoverageState state = classesById.get(classId);
        return state == null ? null : state.probeHits();
    }

    public ExecutionData snapshot() {
        TestTrackingSnapshot attribution = tracker.snapshot();
        List<ClassCoverage> coverages = new ArrayList<>(classesById.size());

        for (Map.Entry<String, ClassCoverageState> entry : classesById.entrySet()) {
            coverages.add(toClassCoverage(entry.getKey(), entry.getValue(), attribution));
        }

        return new ExecutionData(coverages);
    }

    @Override
    public TestTrackingSnapshot snapshotTracker() {
        return tracker.snapshot();
    }

    public int classCount() {
        return classesById.size();
    }

    public void flush() {
        if (classesById.isEmpty()) {
            LOG.warn("No coverage data collected - no classes were instrumented");
            return;
        }

        LOG.info("Flushing coverage data for {} classes", classesById.size());

        Path outputPath = getOutputPath();
        try {
            coverageDataWriter.write(outputPath, snapshot());
            LOG.info("Coverage data written to: {}", outputPath);
        } catch (IOException e) {
            LOG.error("Failed to write coverage data to {}", outputPath, e);
        }
    }

    /**
     * Determines the output file path from system properties.
     */
    private Path getOutputPath() {
        return Path.of(System.getProperty(DEST_FILE_PROPERTY, DEFAULT_DEST_FILE))
                .toAbsolutePath()
                .normalize();
    }

    /**
     * Clears all collected coverage state.
     *
     * <p>This operation is intended for lifecycle boundaries and should not be called concurrently
     * with active probe recording if a fully consistent reset boundary is required.</p>
     */
    public void reset() {
        classesById.clear();
        tracker.reset();
    }

    private List<String> toNullableStrings(Object[] args) {
        List<SerializedArg> serialized = MethodArgumentCapture.capture(args);
        List<String> result = new ArrayList<>(serialized.size());
        for (SerializedArg a : serialized) {
            result.add(a.isNull() ? null : a.value());
        }
        return result;
    }

    private void validateRegistration(String classId, int probeCount, List<ProbeMetadata> metadata) {
        Objects.requireNonNull(classId, "classId must not be null");
        Objects.requireNonNull(metadata, "metadata must not be null");

        if (probeCount < 0) {
            throw new IllegalArgumentException("probeCount must be >= 0");
        }

        if (metadata.size() != probeCount) {
            throw new IllegalArgumentException(
                    "metadata size must match probeCount for class " + classId
                            + " (probeCount=" + probeCount + ", metadata.size=" + metadata.size() + ")");
        }
    }

    private void validateConsistentRegistration(
            String classId,
            int probeCount,
            List<ProbeMetadata> metadata,
            ClassCoverageState existingState
    ) {
        if (existingState.probeCount() != probeCount) {
            throw new IllegalStateException(
                    "Conflicting probe count registration for class " + classId
                            + " (existing=" + existingState.probeCount() + ", new=" + probeCount + ")");
        }

        if (!existingState.metadata().equals(metadata)) {
            throw new IllegalStateException("Conflicting metadata registration for class " + classId);
        }
    }

    private void markProbeAsHit(String classId, int probeId) {
        ClassCoverageState state = classesById.get(classId);
        if (state == null) {
            return;
        }

        boolean[] probeHits = state.probeHits();
        if (probeId < 0 || probeId >= probeHits.length) {
            return;
        }

        probeHits[probeId] = true;
    }

    private void recordInvocationEntry(String classId, String methodName, int probeId, Object[] args) {
        ClassCoverageState state = classesById.get(classId);
        if (state == null) {
            return;
        }

        if (probeId < 0 || probeId >= state.probeCount()) {
            return;
        }

        MethodCallTracker tracker = state.entryTrackersByProbeId().computeIfAbsent(
                probeId,
                ignored -> new MethodCallTracker(methodName)
        );

        tracker.warnIfMethodNameDiffers(methodName, classId, probeId);
        tracker.record(args);
    }

    private ClassCoverage toClassCoverage(String classId, ClassCoverageState state, TestTrackingSnapshot attribution) {
        ClassTestCoverage forClass = attribution.forClass(classId);
        return new ClassCoverage(
                classId,
                state.probeHits().clone(),
                snapshotMethodHits(state),
                snapshotHits(state),
                state.metadata(),
                forClass
        );
    }

    private Map<Integer, ProbeHit> snapshotHits(ClassCoverageState state) {
        AtomicIntegerArray counters = state.counters();
        if (counters.length() == 0) {
            return Collections.emptyMap();
        }

        Map<Integer, ProbeHit> out = new HashMap<>();
        for (int i = 0; i < counters.length(); i++) {
            int c = counters.get(i);
            if (c > 0) {
                out.put(i, new ProbeHit(i, c));
            }
        }

        return out;
    }

    private Map<Integer, MethodHit> snapshotMethodHits(ClassCoverageState state) {
        ConcurrentMap<Integer, MethodCallTracker> trackers = state.entryTrackersByProbeId();
        if (trackers.isEmpty()) {
            return Map.of();
        }

        Map<Integer, MethodHit> methodHitsByProbeId = new LinkedHashMap<>();
        for (Map.Entry<Integer, MethodCallTracker> trackerEntry : trackers.entrySet()) {
            MethodCallTracker tracker = trackerEntry.getValue();
            methodHitsByProbeId.put(
                    trackerEntry.getKey(),
                    new MethodHit(tracker.methodName(), tracker.toInvocationRecords())
            );
        }

        return methodHitsByProbeId;
    }

    /**
     * Mutable per-class coverage state.
     */
    private static final class ClassCoverageState {

        private final boolean[] probeHits;
        private final AtomicIntegerArray counters;
        private final List<ProbeMetadata> metadata;
        private final ConcurrentMap<Integer, MethodCallTracker> entryTrackersByProbeId = new ConcurrentHashMap<>();

        private ClassCoverageState(int probeCount, List<ProbeMetadata> metadata) {
            this.probeHits = new boolean[probeCount];
            this.counters = new AtomicIntegerArray(probeCount);
            this.metadata = List.copyOf(metadata);
        }

        private boolean[] probeHits() {
            return probeHits;
        }

        private AtomicIntegerArray counters() {
            return counters;
        }

        private int probeCount() {
            return probeHits.length;
        }

        private List<ProbeMetadata> metadata() {
            return metadata;
        }

        private ConcurrentMap<Integer, MethodCallTracker> entryTrackersByProbeId() {
            return entryTrackersByProbeId;
        }
    }

    /**
     * Aggregates invocation counts for one method-entry probe.
     */
    private static final class MethodCallTracker {

        private final String methodName;
        private final ConcurrentMap<InvocationKey, AtomicInteger> invocationCounts = new ConcurrentHashMap<>();

        private MethodCallTracker(String methodName) {
            this.methodName = methodName;
        }

        private String methodName() {
            return methodName;
        }

        private void warnIfMethodNameDiffers(String actualMethodName, String classId, int probeId) {
            if (!Objects.equals(this.methodName, actualMethodName)) {
                LOG.warn(
                        "Method name mismatch for classId={}, probeId={}: existing='{}', incoming='{}'",
                        classId, probeId, this.methodName, actualMethodName
                );
            }
        }

        private void record(Object[] rawArgs) {
            InvocationKey key = InvocationKey.from(rawArgs);
            invocationCounts.computeIfAbsent(key, ignored -> new AtomicInteger())
                    .incrementAndGet();
        }

        private List<InvocationRecord> toInvocationRecords() {
            List<InvocationRecord> records =
                    new ArrayList<>(invocationCounts.size());

            for (Map.Entry<InvocationKey, AtomicInteger> entry : invocationCounts.entrySet()) {
                List<String> nullableArgs = toNullableStrings(entry.getKey().arguments());
                records.add(new InvocationRecord(
                        nullableArgs,
                        entry.getValue().get()
                ));
            }

            return records;
        }

        private static List<String> toNullableStrings(List<SerializedArg> serialized) {
            List<String> result = new ArrayList<>(serialized.size());
            for (SerializedArg a : serialized) {
                result.add(a.isNull() ? null : a.value());
            }
            return result;
        }
    }

    /**
     * Key for a single observed argument combination.
     * Uses List<SerializedArg> to distinguish null args from the string "null".
     */
    private static final class InvocationKey {

        private final List<SerializedArg> arguments;

        private InvocationKey(List<SerializedArg> arguments) {
            this.arguments = List.copyOf(arguments);
        }

        private static InvocationKey from(Object[] rawArgs) {
            return new InvocationKey(MethodArgumentCapture.capture(rawArgs));
        }

        private List<SerializedArg> arguments() {
            return arguments;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof InvocationKey that)) {
                return false;
            }
            return arguments.equals(that.arguments);
        }

        @Override
        public int hashCode() {
            return arguments.hashCode();
        }
    }
}
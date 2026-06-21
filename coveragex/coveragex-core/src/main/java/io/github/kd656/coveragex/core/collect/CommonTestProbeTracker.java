package io.github.kd656.coveragex.core.collect;

import io.github.kd656.coveragex.api.context.ProbeExecutionContext;
import io.github.kd656.coveragex.api.context.StandardContextKeys;
import io.github.kd656.coveragex.api.data.AttributedInvocation;
import io.github.kd656.coveragex.api.data.ClassTestCoverage;
import io.github.kd656.coveragex.api.data.TestTrackingSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class CommonTestProbeTracker {

    private static final Logger LOG = LoggerFactory.getLogger(CommonTestProbeTracker.class);

    /**
     * Key deduplicates per (classId, probeId, args, contextId).
     * args may contain null elements, so use unmodifiableList instead of List.copyOf.
     */
    private record TrackingInvocationKey(String classId, int probeId, List<String> args, String contextId) {
        TrackingInvocationKey {
            args = Collections.unmodifiableList(new ArrayList<>(args));
        }
    }

    private final ConcurrentMap<TrackingInvocationKey, ProbeExecutionContext> data = new ConcurrentHashMap<>();

    public void record(String classId, int probeId, List<String> args, ProbeExecutionContext ctx) {
        try {
            data.putIfAbsent(new TrackingInvocationKey(classId, probeId, args, ctx.id()), ctx);
        } catch (Exception e) {
            LOG.warn("Failed to record probe context [classId={}, probeId={}, ctx={}]",
                    classId, probeId, ctx.id(), e);
        }
    }

    /** Insertion-ordered maps keep report output deterministic across runs. */
    public TestTrackingSnapshot snapshot() {
        record GroupKey(String classId, int probeId, List<String> args) {
            GroupKey {
                args = Collections.unmodifiableList(new ArrayList<>(args));
            }
        }

        Map<GroupKey, List<String>> testsByGroup = new LinkedHashMap<>();
        data.forEach((key, ctx) -> {
            GroupKey gk = new GroupKey(key.classId(), key.probeId(), key.args());
            testsByGroup.computeIfAbsent(gk, k -> new ArrayList<>())
                    .add(ctx.get(StandardContextKeys.TEST_METHOD).orElse("unknown"));
        });

        Map<String, Map<Integer, List<AttributedInvocation>>> byClass = new LinkedHashMap<>();
        testsByGroup.forEach((gk, testMethods) -> {
            List<String> sorted = new ArrayList<>(testMethods);
            Collections.sort(sorted);
            AttributedInvocation ai = new AttributedInvocation(gk.args(), sorted);
            byClass.computeIfAbsent(gk.classId(), k -> new LinkedHashMap<>())
                    .computeIfAbsent(gk.probeId(), k -> new ArrayList<>())
                    .add(ai);
        });

        Map<String, ClassTestCoverage> classes = new LinkedHashMap<>();
        byClass.forEach((classId, probeMap) -> {
            Map<Integer, List<AttributedInvocation>> invocations = new LinkedHashMap<>();
            probeMap.forEach((probeId, ais) -> invocations.put(probeId, List.copyOf(ais)));
            classes.put(classId, new ClassTestCoverage(classId, Map.copyOf(invocations)));
        });

        return new TestTrackingSnapshot(Map.copyOf(classes));
    }

    public void reset() {
        data.clear();
    }
}

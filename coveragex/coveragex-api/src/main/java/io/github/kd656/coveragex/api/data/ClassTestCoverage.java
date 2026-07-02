package io.github.kd656.coveragex.api.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public record ClassTestCoverage(String classId, Map<Integer, List<AttributedInvocation>> probeInvocations) {
    public ClassTestCoverage {
        probeInvocations = Map.copyOf(probeInvocations);
    }

    public static ClassTestCoverage empty(String classId) {
        return new ClassTestCoverage(classId, Map.of());
    }

    /**
     * Merges two test-attribution maps for the same class by unioning per-probe
     * invocations.
     *
     * <p>Invocations with identical arguments have their {@code testMethods} lists
     * unioned so {@code TestTrackingStep} shows every test that covered a given
     * probe with a given argument tuple.</p>
     *
     * <p>Implementation uses a {@code Map<List<String>, LinkedHashSet<String>>} keyed
     * on the args tuple for O(1) lookup by args and O(1) test-method dedup, so the
     * overall cost stays linear in the total number of invocations.</p>
     */
    public static ClassTestCoverage merge(ClassTestCoverage a, ClassTestCoverage b) {
        if (!a.classId().equals(b.classId())) {
            throw new IllegalArgumentException(
                    "classId mismatch: " + a.classId() + " vs " + b.classId());
        }
        // probeId -> args-tuple -> ordered union of test methods
        Map<Integer, Map<List<String>, LinkedHashSet<String>>> byProbe = new LinkedHashMap<>();
        accumulate(byProbe, a.probeInvocations());
        accumulate(byProbe, b.probeInvocations());

        Map<Integer, List<AttributedInvocation>> result = new LinkedHashMap<>();
        for (Map.Entry<Integer, Map<List<String>, LinkedHashSet<String>>> probeEntry : byProbe.entrySet()) {
            List<AttributedInvocation> invocations = new ArrayList<>(probeEntry.getValue().size());
            for (Map.Entry<List<String>, LinkedHashSet<String>> argsEntry : probeEntry.getValue().entrySet()) {
                invocations.add(new AttributedInvocation(
                        argsEntry.getKey(),
                        new ArrayList<>(argsEntry.getValue())));
            }
            result.put(probeEntry.getKey(), List.copyOf(invocations));
        }
        return new ClassTestCoverage(a.classId(), result);
    }

    private static void accumulate(
            Map<Integer, Map<List<String>, LinkedHashSet<String>>> target,
            Map<Integer, List<AttributedInvocation>> source) {
        for (Map.Entry<Integer, List<AttributedInvocation>> probeEntry : source.entrySet()) {
            Map<List<String>, LinkedHashSet<String>> byArgs =
                    target.computeIfAbsent(probeEntry.getKey(), k -> new LinkedHashMap<>());
            for (AttributedInvocation invocation : probeEntry.getValue()) {
                byArgs.computeIfAbsent(invocation.args(), k -> new LinkedHashSet<>())
                        .addAll(invocation.testMethods());
            }
        }
    }
}

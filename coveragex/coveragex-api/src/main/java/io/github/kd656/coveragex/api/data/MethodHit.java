package io.github.kd656.coveragex.api.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record MethodHit(String methodName, List<InvocationRecord> invocations) {
    public MethodHit {
        invocations = List.copyOf(invocations);
    }

    /**
     * Merges two method hits for the same method by unioning their invocations.
     *
     * <p>Invocations with identical argument tuples are collapsed and their counts
     * summed, so {@code InvocationTrackingStep}'s "called with" table shows every
     * argument capture from every contributor without duplicated rows.</p>
     */
    public static MethodHit merge(MethodHit a, MethodHit b) {
        if (!a.methodName().equals(b.methodName())) {
            throw new IllegalArgumentException(
                    "methodName mismatch: '" + a.methodName() + "' vs '" + b.methodName() + "'");
        }
        Map<List<String>, Integer> countByArgs = new LinkedHashMap<>();
        for (InvocationRecord ir : a.invocations()) {
            countByArgs.merge(ir.args(), ir.count(), Integer::sum);
        }
        for (InvocationRecord ir : b.invocations()) {
            countByArgs.merge(ir.args(), ir.count(), Integer::sum);
        }
        List<InvocationRecord> merged = new ArrayList<>(countByArgs.size());
        for (Map.Entry<List<String>, Integer> e : countByArgs.entrySet()) {
            merged.add(new InvocationRecord(e.getKey(), e.getValue()));
        }
        return new MethodHit(a.methodName(), merged);
    }
}


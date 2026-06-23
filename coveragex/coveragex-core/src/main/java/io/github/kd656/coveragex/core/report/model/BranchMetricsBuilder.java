package io.github.kd656.coveragex.core.report.model;

import io.github.kd656.coveragex.api.data.ProbeMetadata.BranchDirection;
import io.github.kd656.coveragex.api.data.ProbeMetadata.BranchProbe;
import io.github.kd656.coveragex.api.data.ProbeHit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Pairs {@link BranchProbe} instances into {@link BranchResult} aggregates.
 * One result per decision (one {@code (methodName, line)} key); each result
 * contains one {@link ConditionCase} per operand (one {@code conditionId}
 * within the decision); each case carries the two {@link DirectionOutcome}s
 * for that operand's TRUE/FALSE probes.
 *
 * <p>Extracted from {@link io.github.kd656.coveragex.core.report.logic.ReportingService}
 * so the pairing algorithm has its own home and its own tests.</p>
 *
 * <p>Pairing strategy: within each {@code (methodName, line)} group, probes are
 * grouped by their explicit {@link BranchProbe#conditionId()}. Within each
 * {@code conditionId} group the TRUE-direction probe and the FALSE-direction probe
 * are identified separately. The resulting {@link ConditionCase}s are sorted by
 * {@code conditionId} ascending so that display order is deterministic.</p>
 *
 * <p>Fallback for missing source maps: when every probe in a decision carries the
 * sentinel {@code conditionId = -1}, the builder falls back to positional pairing
 * (sort by {@code probeId}, pair adjacent TRUE / FALSE probes, synthesise
 * 1-based condition ids in source order). Without the fallback every operand of
 * a compound condition would collapse onto the single {@code -1} group and only
 * the last pair's outcomes would survive.</p>
 */
public final class BranchMetricsBuilder {

    private BranchMetricsBuilder() {
    }

    /**
     * Builds the per-decision branch results for a single class.
     *
     * @param branchProbes every {@link BranchProbe} in source order
     * @param probeHits    {@code probeHits[probeId] == true} when the probe
     *                     was taken at least once
     * @param hits         per-probe hit count map, indexed by probe id
     * @return the assembled results, one per decision, in source order
     */
    public static List<BranchResult> build(List<BranchProbe> branchProbes,
                                            boolean[] probeHits,
                                            Map<Integer, ProbeHit> hits) {
        // Group by (methodName, line) preserving insertion order
        Map<DecisionKey, List<BranchProbe>> byDecision = new LinkedHashMap<>();
        for (BranchProbe bp : branchProbes) {
            byDecision.computeIfAbsent(
                    new DecisionKey(bp.methodName(), bp.line()),
                    k -> new ArrayList<>()
            ).add(bp);
        }

        List<BranchResult> results = new ArrayList<>(byDecision.size());
        for (Map.Entry<DecisionKey, List<BranchProbe>> entry : byDecision.entrySet()) {
            DecisionKey key = entry.getKey();
            List<BranchProbe> probes = entry.getValue();

            results.add(buildDecision(key, probes, probeHits, hits));
        }

        return results;
    }

    /**
     * Builds one {@link BranchResult} for the probes that belong to a single
     * decision ({@code (methodName, line)} key).
     *
     * <p>Probes are grouped by {@link BranchProbe#conditionId()}. Within each
     * group the TRUE and FALSE probes are found; {@code argLabels} is read from
     * the TRUE-direction probe when present (both probes in a pair always carry
     * identical labels).</p>
     *
     * @param key       the decision key
     * @param probes    all probes for this decision
     * @param probeHits probe hit flags indexed by probe id
     * @param hits      probe hit counts indexed by probe id
     * @return the assembled {@link BranchResult}
     */
    private static BranchResult buildDecision(DecisionKey key,
                                               List<BranchProbe> probes,
                                               boolean[] probeHits,
                                               Map<Integer, ProbeHit> hits) {
        if (allMissingSourceMap(probes)) {
            return buildDecisionPositionally(key, probes, probeHits, hits);
        }

        // Group by conditionId preserving encounter order for stable output
        Map<Integer, List<BranchProbe>> byConditionId = new LinkedHashMap<>();
        for (BranchProbe bp : probes) {
            byConditionId.computeIfAbsent(bp.conditionId(), k -> new ArrayList<>()).add(bp);
        }

        List<ConditionCase> cases = new ArrayList<>(byConditionId.size());

        for (Map.Entry<Integer, List<BranchProbe>> condEntry : byConditionId.entrySet()) {
            int conditionId = condEntry.getKey();
            List<BranchProbe> condProbes = condEntry.getValue();

            BranchProbe trueProbe = null;
            BranchProbe falseProbe = null;
            for (BranchProbe bp : condProbes) {
                if (bp.direction() == BranchDirection.TRUE) {
                    trueProbe = bp;
                } else {
                    falseProbe = bp;
                }
            }

            String condText = trueProbe != null ? trueProbe.conditionText()
                    : (falseProbe != null ? falseProbe.conditionText() : null);

            // argLabels are identical on both probes for the same condition; prefer TRUE probe
            List<String> argLabels = trueProbe != null ? trueProbe.argLabels()
                    : (falseProbe != null ? falseProbe.argLabels() : List.of());

            DirectionOutcome trueOutcome = toOutcome(trueProbe, probeHits, hits);
            DirectionOutcome falseOutcome = toOutcome(falseProbe, probeHits, hits);

            cases.add(new ConditionCase(conditionId, condText, argLabels, trueOutcome, falseOutcome));
        }

        // Sort by conditionId ascending for deterministic display order
        cases.sort(Comparator.comparingInt(ConditionCase::conditionId));

        return new BranchResult(key.methodName(), key.line(), cases);
    }

    /**
     * Returns {@code true} when every probe in the supplied list carries the
     * {@code conditionId = -1} sentinel — i.e. the decision was instrumented
     * without a source map and has no per-operand id information.
     */
    private static boolean allMissingSourceMap(List<BranchProbe> probes) {
        for (BranchProbe bp : probes) {
            if (bp.conditionId() != -1) {
                return false;
            }
        }
        return !probes.isEmpty();
    }

    /**
     * Pairs probes by source order (probe id ascending) when {@code conditionId}
     * carries the missing-source-map sentinel. Synthesises a 1-based
     * {@code conditionId} per pair so downstream consumers see deterministic
     * ids even when the source map is absent.
     *
     * <p>Assumes the bytecode instrumentation always emits TRUE / FALSE probes
     * in adjacent pairs per operand — the same invariant the positional pairing
     * approach requires.</p>
     */
    private static BranchResult buildDecisionPositionally(DecisionKey key,
                                                            List<BranchProbe> probes,
                                                            boolean[] probeHits,
                                                            Map<Integer, ProbeHit> hits) {
        List<BranchProbe> sorted = probes.stream()
                .sorted(Comparator.comparingInt(BranchProbe::probeId))
                .collect(Collectors.toCollection(ArrayList::new));

        List<ConditionCase> cases = new ArrayList<>();
        BranchProbe truePending = null;
        BranchProbe falsePending = null;
        int syntheticId = 1;
        for (BranchProbe bp : sorted) {
            if (bp.direction() == BranchDirection.TRUE) {
                truePending = bp;
            } else {
                falsePending = bp;
            }
            if (truePending != null && falsePending != null) {
                cases.add(toCase(syntheticId++, truePending, falsePending, probeHits, hits));
                truePending = null;
                falsePending = null;
            }
        }
        // Unpaired tail (shouldn't happen in well-formed bytecode, but stay defensive).
        if (truePending != null || falsePending != null) {
            cases.add(toCase(syntheticId, truePending, falsePending, probeHits, hits));
        }

        return new BranchResult(key.methodName(), key.line(), cases);
    }

    private static ConditionCase toCase(int conditionId,
                                          BranchProbe truePending,
                                          BranchProbe falsePending,
                                          boolean[] probeHits,
                                          Map<Integer, ProbeHit> hits) {
        BranchProbe primary = truePending != null ? truePending : falsePending;
        String condText = primary != null ? primary.conditionText() : null;
        List<String> argLabels = primary != null ? primary.argLabels() : List.of();
        return new ConditionCase(
                conditionId,
                condText,
                argLabels,
                toOutcome(truePending, probeHits, hits),
                toOutcome(falsePending, probeHits, hits));
    }

    private static DirectionOutcome toOutcome(BranchProbe probe,
                                               boolean[] probeHits,
                                               Map<Integer, ProbeHit> hits) {
        if (probe == null) {
            return DirectionOutcome.miss(-1);
        }

        int pid = probe.probeId();
        boolean hit = pid >= 0 && pid < probeHits.length && probeHits[pid];
        ProbeHit ph = hits.get(pid);
        int count = ph != null ? ph.count() : 0;

        return new DirectionOutcome(pid, hit, count);
    }

    /**
     * Compound key for one decision: the enclosing method name and source line.
     */
    private record DecisionKey(String methodName, int line) {
    }
}

package com.coveragex.core.insights;

import com.coveragex.api.data.ExecutionData;
import com.coveragex.api.data.ClassCoverage;
import com.coveragex.api.data.InvocationRecord;
import com.coveragex.api.data.MethodHit;
import com.coveragex.api.data.ProbeMetadata;
import com.coveragex.api.data.ProbeMetadata.BranchDirection;
import com.coveragex.api.data.ProbeMetadata.BranchProbe;
import com.coveragex.api.data.ProbeMetadata.MethodProbe;

import java.util.*;

/**
 * Pure-function insight analysis engine.
 *
 * <p>Applies a fixed set of per-method rules against an {@link ExecutionData} snapshot
 * and returns a list of actionable {@link Insight} records. Produces no I/O and has
 * no side effects.</p>
 *
 * <p>Rule application order within a class:
 * <ol>
 *   <li>Method-level rules (CRITICAL, WARNING, INFO, POSITIVE) are evaluated per method.</li>
 *   <li>POSITIVE/OPTIMAL is only emitted if no WARNING or CRITICAL exists for that method.</li>
 *   <li>DEAD_CLASS is derived after all per-method rules, when every probe in the class is
 *       {@code false}.</li>
 * </ol>
 * </p>
 */
public class InsightEngine {

    /**
     * Analyses the given execution data and returns all insights, sorted by
     * class name, then by line number, then by severity ordinal.
     *
     * @param data execution data read from a {@code .exec} binary
     * @return immutable list of insights; never {@code null}
     */
    public List<Insight> analyze(ExecutionData data) {
        List<Insight> results = new ArrayList<>();

        List<String> classIds = new ArrayList<>(data.classes().keySet());
        Collections.sort(classIds);

        for (String classId : classIds) {
            ClassCoverage cc = data.classes().get(classId);
            List<Insight> classInsights = analyzeClass(classId, cc);
            results.addAll(classInsights);
        }

        return Collections.unmodifiableList(results);
    }

    // -------------------------------------------------------------------------
    // Class-level analysis
    // -------------------------------------------------------------------------

    private List<Insight> analyzeClass(String classId, ClassCoverage cc) {
        List<Insight> insights = new ArrayList<>();

        boolean[] probeHits = cc.probeHits();
        List<ProbeMetadata> metadata = cc.probeMetadata();

        // Check DEAD_CLASS: every probe is false (or no probes at all — no probes means
        // instrumentation produced nothing, so not a dead class by this rule).
        if (probeHits.length > 0) {
            boolean anyHit = false;
            for (boolean hit : probeHits) {
                if (hit) { anyHit = true; break; }
            }
            if (!anyHit) {
                insights.add(new Insight(
                        classId, null, -1,
                        "DEAD_CLASS",
                        Severity.CRITICAL,
                        "Class never loaded during tests",
                        "No probe in this class was ever hit. The class has no test coverage at all."
                ));
                // Still run method-level rules so per-method dead-method insights are produced
            }
        }

        if (metadata.isEmpty()) {
            return insights;
        }

        // Group metadata by method name
        Map<String, List<ProbeMetadata>> byMethod = groupByMethod(metadata);

        for (Map.Entry<String, List<ProbeMetadata>> entry : byMethod.entrySet()) {
            String methodName = entry.getKey();
            List<ProbeMetadata> methodProbes = entry.getValue();

            // Find the MethodProbe for this method (there should be exactly one)
            MethodProbe methodProbe = findMethodProbe(methodProbes);
            if (isImplicitDefaultConstructor(methodName, methodProbes, methodProbe)) {
                continue;
            }
            int methodStartLine = methodProbe != null ? methodProbe.startLine() : -1;

            List<Insight> methodInsights = analyzeMethod(
                    classId, methodName, methodStartLine, methodProbes, probeHits, cc.methodHits());

            insights.addAll(methodInsights);
        }

        // Sort by line then severity
        insights.sort(Comparator.comparingInt(Insight::line).thenComparingInt(a -> a.severity().ordinal()));

        return insights;
    }

    // -------------------------------------------------------------------------
    // Method-level analysis
    // -------------------------------------------------------------------------

    private List<Insight> analyzeMethod(
            String classId,
            String methodName,
            int methodStartLine,
            List<ProbeMetadata> methodProbes,
            boolean[] probeHits,
            Map<Integer, MethodHit> entryProbes) {

        List<Insight> insights = new ArrayList<>();

        // Find the method probe for this method
        MethodProbe methodProbe = findMethodProbe(methodProbes);

        boolean methodHit = methodProbe != null && methodProbe.probeId() < probeHits.length
                && probeHits[methodProbe.probeId()];

        // ── CRITICAL: DEAD_METHOD ─────────────────────────────────────────────
        if (methodProbe != null && !methodHit) {
            insights.add(new Insight(
                    classId, methodName, methodStartLine,
                    "DEAD_METHOD",
                    Severity.CRITICAL,
                    isConstructor(methodName) ? "Constructor never executed" : "Method never executed",
                    isConstructor(methodName)
                            ? "Add a test that creates an instance to exercise this isConstructor."
                            : "Add a test that calls " + methodName + " with representative arguments."
            ));
        }

        // Collect branch probes for this method, grouped by condition line
        List<BranchProbe> branchProbes = collectBranchProbes(methodProbes);

        // Group branch probes by line (i.e. per condition — one T and one F per line)
        Map<Integer, List<BranchProbe>> branchesByLine = groupBranchesByLine(branchProbes);

        // ── Branch-level insights ─────────────────────────────────────────────
        for (Map.Entry<Integer, List<BranchProbe>> branchEntry : branchesByLine.entrySet()) {
            int conditionLine = branchEntry.getKey();
            List<BranchProbe> pair = branchEntry.getValue();

            BranchProbe trueProbe  = findDirection(pair, BranchDirection.TRUE);
            BranchProbe falseProbe = findDirection(pair, BranchDirection.FALSE);

            boolean trueHit  = trueProbe  != null && trueProbe.probeId()  < probeHits.length && probeHits[trueProbe.probeId()];
            boolean falseHit = falseProbe != null && falseProbe.probeId() < probeHits.length && probeHits[falseProbe.probeId()];

            String condText = (trueProbe != null) ? trueProbe.conditionText()
                    : (falseProbe != null ? falseProbe.conditionText() : "");

            // CRITICAL: ZERO_BRANCH_COVERAGE — both directions never hit
            if (!trueHit && !falseHit) {
                insights.add(new Insight(
                        classId, methodName, conditionLine,
                        "ZERO_BRANCH_COVERAGE",
                        Severity.CRITICAL,
                        "Branch never exercised in either direction",
                        "Add tests that make this condition both true and false."
                ));
            } else if (trueHit && !falseHit) {
                // WARNING: MISSING_BRANCH_FALSE
                insights.add(new Insight(
                        classId, methodName, conditionLine,
                        "MISSING_BRANCH_FALSE",
                        Severity.WARNING,
                        "FALSE branch never taken",
                        buildBranchHint(condText, BranchDirection.FALSE)
                ));
                // UNTESTED_NULL_PATH / UNTESTED_EMPTY_PATH sub-checks
                addPathInsight(insights, classId, methodName, conditionLine, condText, BranchDirection.FALSE);

            } else if (!trueHit && falseHit) {
                // WARNING: MISSING_BRANCH_TRUE
                insights.add(new Insight(
                        classId, methodName, conditionLine,
                        "MISSING_BRANCH_TRUE",
                        Severity.WARNING,
                        "TRUE branch never taken",
                        buildBranchHint(condText, BranchDirection.TRUE)
                ));
                addPathInsight(insights, classId, methodName, conditionLine, condText, BranchDirection.TRUE);
            }
            // else: both hit — FULL_BRANCH_COVERAGE (handled at method level below)
        }

        // Count method-level probe stats
        int totalMethodProbes  = countProbesInMethod(methodProbes);
        int hitMethodProbes    = countHitProbes(methodProbes, probeHits);
        int totalBranchProbes  = branchProbes.size();
        int hitBranchProbes    = (int) branchProbes.stream()
                .filter(bp -> bp.probeId() < probeHits.length && probeHits[bp.probeId()])
                .count();

        // Get invocation data for this method
        int totalInvocations   = 0;
        int uniqueArgCombos    = 0;
        if (methodProbe != null) {
            MethodHit mh = entryProbes.get(methodProbe.probeId());
            if (mh != null) {
                for (InvocationRecord inv : mh.invocations()) {
                    totalInvocations += inv.count();
                }
                uniqueArgCombos = mh.invocations().size();
            }
        }

        // ── WARNING: HIGH_COMPLEXITY_LOW_COVERAGE (≥4 branch probes, <50% hit) ─
        if (totalBranchProbes >= 4) {
            double branchCoverage = totalBranchProbes > 0
                    ? (100.0 * hitBranchProbes / totalBranchProbes) : 0.0;
            if (branchCoverage < 50.0) {
                insights.add(new Insight(
                        classId, methodName, methodStartLine,
                        "HIGH_COMPLEXITY_LOW_COVERAGE",
                        Severity.WARNING,
                        "Complex " + callableKind(methodName) + " with low branch coverage",
                        "This " + callableKind(methodName) + " has many branches; prioritise adding tests to cover the untested paths."
                ));
            }
        }

        // ── WARNING: MONOTONE_TEST (≥10 invocations, 1 unique arg combo) ──────
        if (totalInvocations >= 10 && uniqueArgCombos == 1) {
            insights.add(new Insight(
                    classId, methodName, methodStartLine,
                    "MONOTONE_TEST",
                    Severity.WARNING,
                    callableTitle(methodName) + " tested repeatedly with identical arguments",
                    "Consider consolidating redundant test cases or varying argument combinations."
            ));
        }

        // ── INFO: SINGLE_INVOCATION ───────────────────────────────────────────
        if (methodHit && totalInvocations == 1) {
            insights.add(new Insight(
                    classId, methodName, methodStartLine,
                    "SINGLE_INVOCATION",
                    Severity.INFO,
                    callableTitle(methodName) + " exercised only once",
                    "Only one code path has been exercised; consider adding tests with varied inputs."
            ));
        }

        // ── INFO: OVER_TESTED ─────────────────────────────────────────────────
        // 100% probe coverage on method + invocations > 50 + unique arg combinations ≤ 2
        boolean fullProbeCoverage = totalMethodProbes > 0 && hitMethodProbes == totalMethodProbes;
        if (fullProbeCoverage && totalInvocations > 50 && uniqueArgCombos <= 2) {
            insights.add(new Insight(
                    classId, methodName, methodStartLine,
                    "OVER_TESTED",
                    Severity.INFO,
                    "Possibly over-tested — many calls with same arguments",
                    "High call count with very few argument combinations may indicate redundant test cases."
            ));
        }

        // ── INFO: TRIVIAL_HEAVY_TESTING ───────────────────────────────────────
        // Method has exactly 1 probe (no branches) + invocations > 20
        if (totalMethodProbes == 1 && totalInvocations > 20) {
            insights.add(new Insight(
                    classId, methodName, methodStartLine,
                    "TRIVIAL_HEAVY_TESTING",
                    Severity.INFO,
                    "Trivial " + callableKind(methodName) + " invoked excessively in tests",
                    "This " + callableKind(methodName) + " has no branches; the many invocations are likely redundant."
            ));
        }

        // ── POSITIVE: FULL_BRANCH_COVERAGE ───────────────────────────────────
        boolean allBranchesCovered = !branchesByLine.isEmpty() && allBranchesFullyCovered(branchesByLine, probeHits);
        if (allBranchesCovered) {
            insights.add(new Insight(
                    classId, methodName, methodStartLine,
                    "FULL_BRANCH_COVERAGE",
                    Severity.POSITIVE,
                    "Full branch coverage",
                    "All branch directions are covered by tests."
            ));
        }

        // ── POSITIVE: DIVERSE_INVOCATIONS ────────────────────────────────────
        boolean diverseInvocations = uniqueArgCombos >= 3;
        if (diverseInvocations) {
            insights.add(new Insight(
                    classId, methodName, methodStartLine,
                    "DIVERSE_INVOCATIONS",
                    Severity.POSITIVE,
                    "Good test variety — multiple argument paths exercised",
                    "Testing with at least 3 distinct argument combinations is a good sign."
            ));
        }

        // ── POSITIVE: OPTIMAL ────────────────────────────────────────────────
        // Only if no WARNING or CRITICAL exist for this method
        boolean hasWarningOrCritical = insights.stream()
                .anyMatch(i -> i.severity() == Severity.CRITICAL || i.severity() == Severity.WARNING);

        if (fullProbeCoverage && allBranchesCovered && diverseInvocations && !hasWarningOrCritical) {
            insights.add(new Insight(
                    classId, methodName, methodStartLine,
                    "OPTIMAL",
                    Severity.POSITIVE,
                    "Optimal coverage",
                    "100% probe coverage, full branch coverage, and diverse invocations. No action needed."
            ));
        }

        return insights;
    }

    // -------------------------------------------------------------------------
    // Path insight helpers (null / empty path sub-rules under WARNING)
    // -------------------------------------------------------------------------

    /**
     * Adds UNTESTED_NULL_PATH or UNTESTED_EMPTY_PATH insights when the condition text
     * matches well-known patterns. These are additional warnings, not replacements for
     * MISSING_BRANCH_TRUE/FALSE.
     */
    private void addPathInsight(List<Insight> insights, String classId, String methodName,
                                int line, String condText, BranchDirection missedDirection) {
        if (condText == null) return;

        if (condText.contains("null")) {
            insights.add(new Insight(
                    classId, methodName, line,
                    "UNTESTED_NULL_PATH",
                    Severity.WARNING,
                    "Null path not covered",
                    missedDirection == BranchDirection.TRUE
                            ? "Pass a null argument in a test to exercise this path."
                            : "Add a test with a non-null argument."
            ));
        } else if (condText.contains("isEmpty()") || condText.contains("size() == 0")) {
            insights.add(new Insight(
                    classId, methodName, line,
                    "UNTESTED_EMPTY_PATH",
                    Severity.WARNING,
                    "Empty collection path not covered",
                    "Add a test with a non-empty collection."
            ));
        }
    }

    // -------------------------------------------------------------------------
    // Hint generation for branch directions
    // -------------------------------------------------------------------------

    private String buildBranchHint(String condText, BranchDirection missedDirection) {
        if (condText == null) condText = "";
        if (missedDirection == BranchDirection.TRUE) {
            return "Add a test that makes this condition true.";
        } else {
            return "Add a test that makes this condition false.";
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Map<String, List<ProbeMetadata>> groupByMethod(List<ProbeMetadata> metadata) {
        Map<String, List<ProbeMetadata>> result = new HashMap<>();
        for (ProbeMetadata pm : metadata) {
            if (pm == null) continue;
            result.computeIfAbsent(pm.methodName(), k -> new ArrayList<>()).add(pm);
        }
        return result;
    }

    private MethodProbe findMethodProbe(List<ProbeMetadata> probes) {
        for (ProbeMetadata pm : probes) {
            if (pm instanceof MethodProbe mp) return mp;
        }
        return null;
    }

    private List<BranchProbe> collectBranchProbes(List<ProbeMetadata> probes) {
        List<BranchProbe> result = new ArrayList<>();
        for (ProbeMetadata pm : probes) {
            if (pm instanceof BranchProbe bp) result.add(bp);
        }
        return result;
    }

    private Map<Integer, List<BranchProbe>> groupBranchesByLine(List<BranchProbe> branches) {
        Map<Integer, List<BranchProbe>> result = new HashMap<>();
        for (BranchProbe bp : branches) {
            result.computeIfAbsent(bp.line(), k -> new ArrayList<>()).add(bp);
        }
        return result;
    }

    private BranchProbe findDirection(List<BranchProbe> pair, BranchDirection direction) {
        for (BranchProbe bp : pair) {
            if (bp.direction() == direction) return bp;
        }
        return null;
    }

    private int countProbesInMethod(List<ProbeMetadata> probes) {
        return (int) probes.stream().filter(p -> p != null).count();
    }

    private int countHitProbes(List<ProbeMetadata> probes, boolean[] probeHits) {
        int count = 0;
        for (ProbeMetadata pm : probes) {
            if (pm != null && pm.probeId() < probeHits.length && probeHits[pm.probeId()]) {
                count++;
            }
        }
        return count;
    }

    private boolean isConstructor(String methodName) {
        return "<init>".equals(methodName);
    }

    private String callableKind(String methodName) {
        return isConstructor(methodName) ? "constructor" : "method";
    }

    private String callableTitle(String methodName) {
        return isConstructor(methodName) ? "constructor" : "Method";
    }

    private boolean isImplicitDefaultConstructor(String methodName, List<ProbeMetadata> methodProbes, MethodProbe methodProbe) {
        if (!isConstructor(methodName) || methodProbe == null) {
            return false;
        }

        int nonMethodProbeCount = 0;
        for (ProbeMetadata pm : methodProbes) {
            if (!(pm instanceof MethodProbe)) {
                nonMethodProbeCount++;
            }
        }

        return methodProbe.startLine() == methodProbe.endLine() && nonMethodProbeCount == 1;
    }

    private boolean allBranchesFullyCovered(Map<Integer, List<BranchProbe>> branchesByLine,
                                            boolean[] probeHits) {
        for (List<BranchProbe> pair : branchesByLine.values()) {
            BranchProbe trueProbe  = findDirection(pair, BranchDirection.TRUE);
            BranchProbe falseProbe = findDirection(pair, BranchDirection.FALSE);
            boolean trueHit  = trueProbe  != null && trueProbe.probeId()  < probeHits.length && probeHits[trueProbe.probeId()];
            boolean falseHit = falseProbe != null && falseProbe.probeId() < probeHits.length && probeHits[falseProbe.probeId()];
            if (!trueHit || !falseHit) return false;
        }
        return true;
    }
}

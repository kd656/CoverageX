package io.github.kd656.coveragex.core.report.logic;

import io.github.kd656.coveragex.api.data.ClassCoverage;
import io.github.kd656.coveragex.api.data.ExecutionData;
import io.github.kd656.coveragex.api.data.InvocationRecord;
import io.github.kd656.coveragex.api.data.MethodHit;
import io.github.kd656.coveragex.api.data.ProbeMetadata;
import io.github.kd656.coveragex.api.data.ProbeMetadata.BranchProbe;
import io.github.kd656.coveragex.api.data.ProbeMetadata.MethodProbe;
import io.github.kd656.coveragex.core.report.ReportInput;
import io.github.kd656.coveragex.core.report.ReportScope;
import io.github.kd656.coveragex.core.report.model.BranchMetricsBuilder;
import io.github.kd656.coveragex.core.report.model.BranchResult;
import io.github.kd656.coveragex.core.report.model.ClassMetrics;
import io.github.kd656.coveragex.core.report.model.Coverage;
import io.github.kd656.coveragex.core.report.model.LineStatus;
import io.github.kd656.coveragex.core.report.model.MethodMetrics;
import io.github.kd656.coveragex.core.report.model.ReportModel;
import io.github.kd656.coveragex.core.report.model.SummaryMetrics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Default implementation of {@link ReportModelFactory}.
 *
 * <p>This class holds the model-building logic previously inlined in
 * {@code ReportingService.buildInitialModel}. Behavior is unchanged; extracting it
 * lets aggregation flows build scoped models from multiple inputs while single-module
 * callers keep going through {@link #build(ExecutionData)}.</p>
 */
public final class DefaultReportModelFactory implements ReportModelFactory {

    @Override
    public ReportModel build(ExecutionData data) {
        List<ClassMetrics> classMetricsList = buildClassMetricsList(data);
        SummaryMetrics summary = new SummaryMetrics(
                data.totalProbes(),
                data.executedProbes(),
                getGlobalPct(classMetricsList),
                data.classCount());
        return new ReportModel(classMetricsList, summary);
    }

    @Override
    public ReportModel build(List<ReportInput> inputs) {
        if (inputs.isEmpty()) {
            throw new IllegalArgumentException("inputs must not be empty");
        }
        List<ReportScope> scopes = new ArrayList<>(inputs.size());
        List<ClassMetrics> allClassMetrics = new ArrayList<>();
        int totalProbes = 0;
        int executedProbes = 0;
        int classCount = 0;
        for (ReportInput input : inputs) {
            ExecutionData data = input.executionData();
            List<ClassMetrics> perScope = buildClassMetricsList(data);
            SummaryMetrics perScopeSummary = new SummaryMetrics(
                    data.totalProbes(),
                    data.executedProbes(),
                    getGlobalPct(perScope),
                    data.classCount());
            scopes.add(new ReportScope(
                    input.scopeId(),
                    input.displayName(),
                    input.sourceDirectory(),
                    perScope,
                    perScopeSummary));
            allClassMetrics.addAll(perScope);
            totalProbes += data.totalProbes();
            executedProbes += data.executedProbes();
            classCount += data.classCount();
        }
        SummaryMetrics aggregate = new SummaryMetrics(
                totalProbes,
                executedProbes,
                getGlobalPct(allClassMetrics),
                classCount);
        return ReportModel.ofScopes(scopes, aggregate);
    }

    // -------------------------------------------------------------------------
    // Bootstrap pass: ExecutionData -> List<ClassMetrics>
    // -------------------------------------------------------------------------

    private List<ClassMetrics> buildClassMetricsList(ExecutionData data) {
        return data.classes().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> buildClassMetrics(e.getKey(), e.getValue()))
                .toList();
    }

    private double getGlobalPct(List<ClassMetrics> classMetricsList) {
        long totalLines = 0, coveredLines = 0;
        for (ClassMetrics cm : classMetricsList) {
            for (LineStatus ls : cm.lines()) {
                if (ls.coverage() != Coverage.NOT_EXECUTABLE) {
                    totalLines++;
                    if (ls.coverage() == Coverage.HIT) {
                        coveredLines++;
                    }
                }
            }
        }
        return totalLines > 0 ? (100.0 * coveredLines / totalLines) : 0.0;
    }

    private ClassMetrics buildClassMetrics(String classId, ClassCoverage cc) {
        boolean[] probeHits = cc.probeHits();
        List<ProbeMetadata> metadata = cc.probeMetadata();
        Map<Integer, MethodHit> entryProbes = cc.methodHits();

        Map<String, MethodProbe> methodProbeByKey = new LinkedHashMap<>();
        Map<String, List<ProbeMetadata>> probesByKey = new LinkedHashMap<>();
        Map<Integer, List<ProbeMetadata>> probesByLine = new HashMap<>();
        List<BranchProbe> allBranchProbes = new ArrayList<>();
        long totalBranches = 0, coveredBranches = 0, coveredMethods = 0;

        for (ProbeMetadata pm : metadata) {
            if (pm == null) continue;

            if (pm instanceof MethodProbe mp) {
                String key = MethodMetrics.methodKey(mp);
                methodProbeByKey.put(key, mp);
                probesByKey.computeIfAbsent(key, k -> new ArrayList<>()).add(pm);
                if (isProbeHit(mp.probeId(), probeHits)) coveredMethods++;
            }

            int lineKey = pm.lineNumber();
            if (lineKey > 0) {
                probesByLine.computeIfAbsent(lineKey, k -> new ArrayList<>()).add(pm);
            }
        }

        for (ProbeMetadata pm : metadata) {
            if (pm == null || pm instanceof MethodProbe) continue;

            findOwningMethod(pm, methodProbeByKey.values()).ifPresent(mp -> {
                String key = MethodMetrics.methodKey(mp);
                probesByKey.computeIfAbsent(key, k -> new ArrayList<>()).add(pm);
            });

            if (pm instanceof BranchProbe bp) {
                totalBranches++;
                if (isProbeHit(bp.probeId(), probeHits)) coveredBranches++;
                allBranchProbes.add(bp);
            }
        }

        List<BranchResult> branchResults = BranchMetricsBuilder.build(allBranchProbes, probeHits, cc.hits());
        List<LineStatus> lineStatuses = buildLineStatuses(probesByLine, probeHits);
        List<MethodMetrics> methodMetricsList = buildMethodMetrics(
                probesByKey, methodProbeByKey, probeHits, entryProbes);

        long totalLines = 0, coveredLines = 0;
        for (LineStatus ls : lineStatuses) {
            if (ls.coverage() != Coverage.NOT_EXECUTABLE) {
                totalLines++;
                if (ls.coverage() == Coverage.HIT) {
                    coveredLines++;
                }
            }
        }

        double linePct   = totalLines    > 0 ? (100.0 * coveredLines    / totalLines)    : 0.0;
        double branchPct = totalBranches > 0 ? (100.0 * coveredBranches / totalBranches) : 0.0;
        long   totalMethods = methodProbeByKey.size();
        double methodPct = totalMethods  > 0 ? (100.0 * coveredMethods  / totalMethods)  : 0.0;

        return new ClassMetrics(classId, simpleClassName(classId), packageName(classId),
                linePct, branchPct, methodPct,
                methodMetricsList, branchResults, lineStatuses, metadata, cc.testAttribution());
    }

    private List<LineStatus> buildLineStatuses(Map<Integer, List<ProbeMetadata>> probesByLine,
                                                boolean[] probeHits) {
        List<LineStatus> statuses = new ArrayList<>();
        List<Integer> sortedLines = new ArrayList<>(probesByLine.keySet());
        Collections.sort(sortedLines);

        for (int lineNum : sortedLines) {
            List<ProbeMetadata> lineProbes = probesByLine.get(lineNum);
            boolean anyHit = false, anyMiss = false;

            for (ProbeMetadata pm : lineProbes) {
                boolean hit = isProbeHit(pm.probeId(), probeHits);
                if (hit) anyHit = true; else anyMiss = true;
            }

            Coverage coverage;
            if (!anyHit) {
                coverage = Coverage.MISS;
            } else if (!anyMiss) {
                coverage = Coverage.HIT;
            } else {
                coverage = Coverage.PARTIAL_BRANCH;
            }

            statuses.add(new LineStatus(lineNum, coverage));
        }
        return statuses;
    }

    private List<MethodMetrics> buildMethodMetrics(Map<String, List<ProbeMetadata>> probesByKey,
                                                    Map<String, MethodProbe> methodProbeByKey,
                                                    boolean[] probeHits,
                                                    Map<Integer, MethodHit> entryProbes) {
        List<MethodMetrics> result = new ArrayList<>();

        for (Map.Entry<String, List<ProbeMetadata>> entry : probesByKey.entrySet()) {
            String key = entry.getKey();
            List<ProbeMetadata> methodProbes = entry.getValue();
            MethodProbe methodProbe = methodProbeByKey.get(key);
            if (methodProbe == null) continue;

            String methodName = methodProbe.methodName();

            int probeCount = 0, hitProbeCount = 0, branchProbeCount = 0, hitBranchProbeCount = 0;
            for (ProbeMetadata pm : methodProbes) {
                if (pm == null) continue;
                probeCount++;
                boolean hit = isProbeHit(pm.probeId(), probeHits);
                if (hit) hitProbeCount++;
                if (pm instanceof BranchProbe) {
                    branchProbeCount++;
                    if (hit) hitBranchProbeCount++;
                }
            }

            List<InvocationRecord> invocations = List.of();
            int hitCount = 0;
            MethodHit mh = entryProbes.get(methodProbe.probeId());
            if (mh != null) {
                invocations = mh.invocations();
                for (InvocationRecord inv : invocations) hitCount += inv.count();
            }

            boolean constructor = isConstructor(methodName);
            boolean implicitDefaultConstructor = isImplicitDefaultConstructor(methodName, methodProbes);

            result.add(new MethodMetrics(methodName, constructor, implicitDefaultConstructor,
                    methodProbe.startLine(), methodProbe.endLine(),
                    hitCount, probeCount, hitProbeCount, branchProbeCount, hitBranchProbeCount,
                    invocations));
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String simpleClassName(String classId) {
        int slash = classId.lastIndexOf('/');
        return slash >= 0 ? classId.substring(slash + 1) : classId;
    }

    private String packageName(String classId) {
        int slash = classId.lastIndexOf('/');
        if (slash < 0) return "";
        return classId.substring(0, slash).replace('/', '.');
    }

    private boolean isConstructor(String methodName) {
        return "<init>".equals(methodName);
    }

    private boolean isProbeHit(int probeId, boolean[] probeHits) {
        return probeId >= 0 && probeId < probeHits.length && probeHits[probeId];
    }

    private Optional<MethodProbe> findOwningMethod(ProbeMetadata probe, Collection<MethodProbe> methods) {
        int line = probe.lineNumber();
        return methods.stream()
                .filter(mp -> mp.methodName().equals(probe.methodName()))
                .filter(mp -> line >= mp.startLine() && line <= mp.endLine())
                .min(Comparator
                        .comparingInt((MethodProbe mp) -> mp.endLine() - mp.startLine())
                        .thenComparingInt(MethodProbe::startLine));
    }

    private boolean isImplicitDefaultConstructor(String methodName, List<ProbeMetadata> methodProbes) {
        if (!isConstructor(methodName)) {
            return false;
        }

        MethodProbe methodProbe = null;
        int nonMethodProbeCount = 0;
        for (ProbeMetadata pm : methodProbes) {
            if (pm instanceof MethodProbe mp) {
                methodProbe = mp;
            } else {
                nonMethodProbeCount++;
            }
        }

        return methodProbe != null
                && methodProbe.startLine() == methodProbe.endLine()
                && nonMethodProbeCount == 1;
    }
}

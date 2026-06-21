package com.coveragex.core.report.logic;

import com.coveragex.api.data.ExecutionData;
import com.coveragex.api.data.ClassCoverage;
import com.coveragex.api.data.InvocationRecord;
import com.coveragex.api.data.MethodHit;
import com.coveragex.api.data.ProbeHit;
import com.coveragex.api.data.ProbeMetadata;
import com.coveragex.api.data.ProbeMetadata.BranchDirection;
import com.coveragex.api.data.ProbeMetadata.BranchProbe;
import com.coveragex.api.data.ProbeMetadata.MethodProbe;
import com.coveragex.core.report.ReportConfig;
import com.coveragex.core.report.ReportView;
import com.coveragex.core.report.model.*;
import com.coveragex.core.report.pipeline.ReportPipelineStep;
import com.coveragex.core.report.pipeline.steps.*;
import com.coveragex.core.report.views.ConsoleReportView;
import com.coveragex.core.report.views.HtmlReportRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class ReportingService {

    private static final Logger LOG = LoggerFactory.getLogger(ReportingService.class);

    private final Map<ReportingType, ReportView> views;

    public ReportingService() {
        this(Map.of(
            ReportingType.CONSOLE, new ConsoleReportView(),
            ReportingType.HTML,    new HtmlReportRenderer()
        ));
    }

    public ReportingService(Map<ReportingType, ReportView> views) {
        this.views = views;
    }

    public void report(ReportConfig config, ExecutionData data) {
        ReportModel model = buildInitialModel(data);

        runPipeline(config, model);

        for (ReportingType format : config.reportFormats()) {
            ReportView view = views.get(format);
            if (view == null) {
                LOG.warn("No view registered for format '{}' — skipping.", format);
            } else {
                view.render(model, config.context());
            }
        }
    }

    // -------------------------------------------------------------------------
    // Bootstrap pass: ExecutionData -> ReportModel with ClassMetrics
    // -------------------------------------------------------------------------

    private ReportModel buildInitialModel(ExecutionData data) {
        List<ClassMetrics> classMetricsList = data.classes().entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> buildClassMetrics(e.getKey(), e.getValue()))
            .toList();

        double globalPct = getGlobalPct(classMetricsList);
        SummaryMetrics summary = new SummaryMetrics(
            data.totalProbes(), data.executedProbes(), globalPct, data.classCount());

        return new ReportModel(classMetricsList, summary);
    }

    private double getGlobalPct(List<ClassMetrics> classMetricsList) {
        long totalLines = 0, coveredLines = 0;
        for (ClassMetrics cm : classMetricsList) {
            for (LineStatus ls : cm.lines()) {
                if (ls.coverage() != Coverage.NOT_EXECUTABLE) {
                    totalLines++;
                    if (ls.coverage() == Coverage.HIT || ls.coverage() == Coverage.PARTIAL_BRANCH) {
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
        Map<String, Map<Integer, List<BranchProbe>>> branchByMethodLine = new LinkedHashMap<>();
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
                branchByMethodLine
                    .computeIfAbsent(bp.methodName(), k -> new LinkedHashMap<>())
                    .computeIfAbsent(bp.line(), k -> new ArrayList<>())
                    .add(bp);
            }
        }

        List<BranchResult> branchResults = buildBranchResults(branchByMethodLine, probeHits, cc.hits());
        List<LineStatus> lineStatuses = buildLineStatuses(probesByLine, probeHits);
        List<MethodMetrics> methodMetricsList = buildMethodMetrics(
            probesByKey, methodProbeByKey, probeHits, entryProbes);

        long totalLines = 0, coveredLines = 0;
        for (LineStatus ls : lineStatuses) {
            if (ls.coverage() != Coverage.NOT_EXECUTABLE) {
                totalLines++;
                if (ls.coverage() == Coverage.HIT || ls.coverage() == Coverage.PARTIAL_BRANCH) {
                    coveredLines++;
                }
            }
        }

        double linePct    = totalLines    > 0 ? (100.0 * coveredLines   / totalLines)    : 0.0;
        double branchPct  = totalBranches > 0 ? (100.0 * coveredBranches / totalBranches) : 0.0;
        long   totalMethods = methodProbeByKey.size();
        double methodPct  = totalMethods  > 0 ? (100.0 * coveredMethods  / totalMethods)  : 0.0;

        return new ClassMetrics(classId, simpleClassName(classId), packageName(classId),
            linePct, branchPct, methodPct,
            methodMetricsList, branchResults, lineStatuses, metadata, cc.testAttribution());
    }

    private List<BranchResult> buildBranchResults(
            Map<String, Map<Integer, List<BranchProbe>>> byMethodLine,
            boolean[] probeHits,
            Map<Integer, ProbeHit> hits) {

        List<BranchResult> results = new ArrayList<>();

        for (Map.Entry<String, Map<Integer, List<BranchProbe>>> methodEntry : byMethodLine.entrySet()) {
            String methodName = methodEntry.getKey();
            for (Map.Entry<Integer, List<BranchProbe>> lineEntry : methodEntry.getValue().entrySet()) {
                int line = lineEntry.getKey();
                List<BranchProbe> allProbes = lineEntry.getValue();

                Map<BranchDirection, List<BranchProbe>> byDirection = allProbes.stream()
                    .sorted(Comparator.comparingInt(BranchProbe::probeId))
                    .collect(Collectors.groupingBy(BranchProbe::direction));
                List<BranchProbe> trueProbes  = byDirection.getOrDefault(BranchDirection.TRUE,  Collections.emptyList());
                List<BranchProbe> falseProbes = byDirection.getOrDefault(BranchDirection.FALSE, Collections.emptyList());

                int pairCount = Math.max(trueProbes.size(), falseProbes.size());
                for (int i = 0; i < pairCount; i++) {
                    BranchProbe trueProbe  = i < trueProbes.size()  ? trueProbes.get(i)  : null;
                    BranchProbe falseProbe = i < falseProbes.size() ? falseProbes.get(i) : null;

                    boolean trueHit  = trueProbe  != null && isProbeHit(trueProbe.probeId(), probeHits);
                    boolean falseHit = falseProbe != null && isProbeHit(falseProbe.probeId(), probeHits);
                    int trueCount  = countOf(hits, trueProbe);
                    int falseCount = countOf(hits, falseProbe);
                    String condText = trueProbe != null ? trueProbe.conditionText()
                        : (falseProbe != null ? falseProbe.conditionText() : "");

                    results.add(new BranchResult(methodName, line, condText, trueHit, falseHit, trueCount, falseCount));
                }
            }
        }
        return results;
    }

    private int countOf(Map<Integer, ProbeHit> hits, BranchProbe probe) {
        if (probe == null) return 0;
        ProbeHit h = hits.get(probe.probeId());
        return h == null ? 0 : h.count();
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
            if (methodProbe == null) continue; // skip if no method entry probe

            // Extract the simple display name from the probe, not from the compound key
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
    // Pipeline
    // -------------------------------------------------------------------------

    private void runPipeline(ReportConfig config, ReportModel model) {
        List<ReportPipelineStep> all = List.of(
            new InvocationTrackingStep(),
            new TestTrackingStep(),
            new InsightsStep(),
            new SuggestionsStep(),
            new MCDCStep(),
            new OverCoverageStep()
        );

        List<ReportPipelineStep> enabled = all.stream()
            .filter(step -> config.isStepEnabled(step.stepId()))
            .toList();

        for (int i = 0; i < enabled.size() - 1; i++) {
            enabled.get(i).setNext(enabled.get(i + 1));
        }

        if (!enabled.isEmpty()) {
            enabled.getFirst().handle(model);
        }
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

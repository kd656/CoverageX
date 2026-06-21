package com.coveragex.core.report.views.assembler;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.coveragex.api.data.InvocationRecord;
import com.coveragex.api.data.ProbeMetadata;
import com.coveragex.api.data.ProbeMetadata.BranchProbe;
import com.coveragex.api.data.AttributedInvocation;
import com.coveragex.api.data.TestTrackingSnapshot;
import com.coveragex.core.insights.Insight;
import com.coveragex.core.insights.Severity;
import com.coveragex.core.report.model.*;
import com.coveragex.core.report.pipeline.results.InvocationReport;
import com.coveragex.core.report.pipeline.results.InvocationResult;
import com.coveragex.core.report.views.html.payload.*;
import com.coveragex.core.report.views.probe.BranchHintGenerator;
import com.coveragex.core.report.views.probe.LineClassification;
import com.coveragex.core.report.views.probe.ProbeIndex;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Converts ClassMetrics + insights + model data into a .data.js file content string.
 * The file contains a single CoverageX.registerClass(id, payload) call that the browser
 * loads by injecting a script tag. FastJson2 serialises the typed payload records;
 * Java never manually constructs JSON strings.
 */
public class ClassDataSerializer {

    // Coverage code constants — match spec section 3.3
    private static final int CODE_NOT_EXECUTABLE = 0;
    private static final int CODE_HIT             = 1;
    private static final int CODE_MISS            = 2;
    private static final int CODE_PARTIAL_BRANCH  = 3;

    private static final JSONWriter.Feature[] OMIT_FEATURES = {
        JSONWriter.Feature.NotWriteEmptyArray,
        JSONWriter.Feature.NotWriteDefaultValue
    };

    /**
     * Builds the full .data.js file content for one class.
     *
     * @param cm          class metrics
     * @param sourceFile  path to the .java source file, or null when not available
     * @param insights    coverage insights for this class (may be empty)
     * @param model       full report model (used for invocation and test-tracking data)
     * @param sectionId   the stable section identifier (classId with '/' replaced by '-')
     * @return complete .data.js file content as a string
     */
    public String serialize(ClassMetrics cm, Path sourceFile, List<Insight> insights,
                            ReportModel model, String sectionId) throws IOException {
        Map<Severity, Long> countBySeverity = insights.stream()
            .collect(Collectors.groupingBy(Insight::severity, Collectors.counting()));

        List<SourceLinePayload> sourceLines = null;
        List<MethodFallbackPayload> methodFallbackRows = null;

        if (sourceFile != null) {
            sourceLines = buildSourceLines(cm, sourceFile, sectionId, model);
        } else {
            methodFallbackRows = buildMethodFallbackRows(cm);
        }

        List<InsightPayload> insightPayloads = buildInsightPayloads(cm, insights);

        ClassCoveragePayload payload = new ClassCoveragePayload(
            cm.simpleName(),
            cm.packageName(),
            cm.lineCoveragePercent(),
            (int) (long) countBySeverity.getOrDefault(Severity.CRITICAL, 0L),
            (int) (long) countBySeverity.getOrDefault(Severity.WARNING,  0L),
            (int) (long) countBySeverity.getOrDefault(Severity.INFO,     0L),
            (int) (long) countBySeverity.getOrDefault(Severity.POSITIVE, 0L),
            sourceLines,
            methodFallbackRows,
            insightPayloads.isEmpty() ? null : insightPayloads
        );

        return "CoverageX.registerClass("
            + JSON.toJSONString(sectionId)
            + ","
            + JSON.toJSONString(payload, OMIT_FEATURES)
            + ");";
    }

    // -----------------------------------------------------------------------
    // Source lines
    // -----------------------------------------------------------------------

    private List<SourceLinePayload> buildSourceLines(ClassMetrics cm, Path sourceFile,
                                                     String sectionId, ReportModel model) throws IOException {
        List<String> rawLines = Files.readAllLines(sourceFile, StandardCharsets.UTF_8);
        ProbeIndex index = ProbeIndex.build(cm.probeMetadata());

        Map<Integer, Coverage> lineCoverage = new HashMap<>();
        for (LineStatus ls : cm.lines()) lineCoverage.put(ls.lineNumber(), ls.coverage());

        Map<String, Integer> methodHits = new HashMap<>();
        for (MethodMetrics mm : cm.methods()) methodHits.put(mm.methodKey(), mm.hitCount());
        Map<String, MethodMetrics> metricsByMethod = cm.methods().stream()
            .collect(Collectors.toMap(MethodMetrics::methodKey, m -> m, (left, right) -> left));

        boolean showInvocations = model.get(InvocationResult.class).isPresent();
        Map<String, InvocationReport> invocationReports = buildInvocationReports(cm.classId(), model);

        String prevMethodKey = null;
        List<SourceLinePayload> result = new ArrayList<>();

        for (int lineNum = 1; lineNum <= rawLines.size(); lineNum++) {
            List<ProbeMetadata> lineProbes = index.probesAt(lineNum);
            Optional<ProbeMetadata.MethodProbe> coveringMethod = index.methodAt(lineNum);
            Coverage coverage = lineCoverage.getOrDefault(lineNum, Coverage.NOT_EXECUTABLE);
            LineClassification lc = LineClassification.of(lineProbes);

            String currentMethodKey = null;
            if (coveringMethod.isPresent() && coveringMethod.get().startLine() == lineNum) {
                currentMethodKey = MethodMetrics.methodKey(coveringMethod.get());
            }

            boolean separator = currentMethodKey != null
                && prevMethodKey != null
                && !currentMethodKey.equals(prevMethodKey);

            if (currentMethodKey != null) {
                prevMethodKey = currentMethodKey;
            }

            MethodMarkerPayload marker = null;
            if (showInvocations && lc.isMethodEntry() && lc.firstMethodProbe() != null
                    && isVisibleCallable(MethodMetrics.methodKey(lc.firstMethodProbe()), metricsByMethod)) {
                marker = buildMethodMarker(cm, lc, invocationReports, methodHits, metricsByMethod, model);
            }

            List<BranchBadgePayload> badges = lc.hasBranch()
                ? buildBranchBadges(lineProbes, cm)
                : null;

            String hitDisplay = showInvocations
                ? computeHitDisplay(lineProbes, coverage, coveringMethod.orElse(null),
                                     invocationReports, methodHits, metricsByMethod)
                : null;

            int coverageCode = toCoverageCode(coverage);

            result.add(new SourceLinePayload(
                lineNum,
                rawLines.get(lineNum - 1),
                coverageCode,
                (hitDisplay != null && !hitDisplay.isEmpty()) ? hitDisplay : null,
                separator ? 1 : null,
                marker,
                (badges != null && !badges.isEmpty()) ? badges : null
            ));
        }

        return result;
    }

    private MethodMarkerPayload buildMethodMarker(ClassMetrics cm, LineClassification lc,
                                                  Map<String, InvocationReport> invocationReports,
                                                  Map<String, Integer> methodHits,
                                                  Map<String, MethodMetrics> metricsByMethod,
                                                  ReportModel model) {
        ProbeMetadata.MethodProbe mp = lc.firstMethodProbe();
        String key = MethodMetrics.methodKey(mp);
        InvocationReport report = invocationReports.get(key);
        int calls = report != null ? report.totalCallCount()
            : methodHits.getOrDefault(key, 0);

        TestTrackingSnapshot snapshot = model.getTestTrackingSnapshot();
        List<AttributedInvocation> attributed = snapshot.forClass(cm.classId())
            .probeInvocations().getOrDefault(mp.probeId(), List.of());

        List<InvocationPayload> invocations = buildInvocationPayloads(attributed, report);

        return new MethodMarkerPayload(
            displayCallableName(cm, metricsByMethod.get(key)),
            calls,
            invocations.isEmpty() ? null : invocations
        );
    }

    private List<InvocationPayload> buildInvocationPayloads(List<AttributedInvocation> attributed,
                                                             InvocationReport report) {
        if (!attributed.isEmpty() && report != null) {
            Map<List<String>, Integer> countByArgs = report.argCombinations().stream()
                .collect(Collectors.toMap(
                    InvocationRecord::args,
                    InvocationRecord::count,
                    Integer::sum
                ));
            return attributed.stream()
                .map(a -> new InvocationPayload(
                    a.testMethods().isEmpty() ? null : a.testMethods(),
                    a.args().isEmpty() ? null : a.args(),
                    countByArgs.getOrDefault(a.args(), 0)))
                .toList();
        }
        if (report != null) {
            return report.argCombinations().stream()
                .map(ir -> new InvocationPayload(
                    null,
                    ir.args().isEmpty() ? null : ir.args(),
                    ir.count()))
                .toList();
        }
        return List.of();
    }

    private List<BranchBadgePayload> buildBranchBadges(List<ProbeMetadata> lineProbes,
                                                         ClassMetrics cm) {
        int probeLine = lineProbes.stream()
            .filter(p -> p instanceof BranchProbe)
            .map(p -> ((BranchProbe) p).line())
            .findFirst().orElse(-1);
        if (probeLine < 0) return Collections.emptyList();

        List<BranchResult> branchResults = cm.branches().stream()
            .filter(br -> br.line() == probeLine)
            .toList();
        if (branchResults.isEmpty()) return Collections.emptyList();

        List<BranchConditionPayload> conditions = branchResults.stream()
            .map(br -> new BranchConditionPayload(
                br.conditionText(),
                br.trueHit()  ? 1 : 0,
                br.falseHit() ? 1 : 0,
                br.trueCount(),
                br.falseCount(),
                nullIfEmpty(BranchHintGenerator.hint(br.conditionText(), true,  br.trueHit(), br.falseHit())),
                nullIfEmpty(BranchHintGenerator.hint(br.conditionText(), false, br.trueHit(), br.falseHit()))
            ))
            .toList();

        boolean anyTrueCovered = false, allTrueCovered = true;
        boolean anyFalseCovered = false, allFalseCovered = true;
        for (BranchConditionPayload c : conditions) {
            if (c.trueHit()  == 1) anyTrueCovered  = true; else allTrueCovered  = false;
            if (c.falseHit() == 1) anyFalseCovered = true; else allFalseCovered = false;
        }

        int trueCovCode  = toCondCovCode(allTrueCovered,  anyTrueCovered);
        int falseCovCode = toCondCovCode(allFalseCovered, anyFalseCovered);

        // Build test lists from test-tracking data in BranchResult
        // (BranchResult doesn't carry per-test data directly; tests come from test tracking)
        // For now, tests list is empty — test tracking data is not in BranchResult
        List<BranchTestPayload> emptyTests = null;

        return List.of(
            new BranchBadgePayload(1, trueCovCode,  conditions, emptyTests),
            new BranchBadgePayload(0, falseCovCode, conditions, emptyTests)
        );
    }

    private String computeHitDisplay(List<ProbeMetadata> lineProbes, Coverage coverage,
                                     ProbeMetadata.MethodProbe coveringMethod,
                                     Map<String, InvocationReport> invocationReports,
                                     Map<String, Integer> methodHits,
                                     Map<String, MethodMetrics> metricsByMethod) {
        if (lineProbes.isEmpty()) return "";
        if (coverage == Coverage.MISS) return "0x";
        if (coveringMethod != null && isVisibleCallable(MethodMetrics.methodKey(coveringMethod), metricsByMethod)) {
            String key = MethodMetrics.methodKey(coveringMethod);
            InvocationReport r = invocationReports.get(key);
            int n = r != null ? r.totalCallCount() : methodHits.getOrDefault(key, 0);
            if (n > 0) return n + "x";
        }
        for (ProbeMetadata pm : lineProbes) {
            if (pm instanceof ProbeMetadata.MethodProbe mp && isVisibleCallable(MethodMetrics.methodKey(mp), metricsByMethod)) {
                String key = MethodMetrics.methodKey(mp);
                InvocationReport r = invocationReports.get(key);
                int n = r != null ? r.totalCallCount() : methodHits.getOrDefault(key, 0);
                if (n > 0) return n + "x";
            }
        }
        return (coverage == Coverage.HIT || coverage == Coverage.PARTIAL_BRANCH) ? "✓" : "";
    }

    // -----------------------------------------------------------------------
    // Method fallback rows (no source file)
    // -----------------------------------------------------------------------

    private List<MethodFallbackPayload> buildMethodFallbackRows(ClassMetrics cm) {
        List<MethodFallbackPayload> rows = new ArrayList<>();
        for (MethodMetrics mm : cm.methods()) {
            if (mm.isImplicitDefaultConstructor()) {
                continue;
            }
            boolean hit = mm.hitCount() > 0;
            rows.add(new MethodFallbackPayload(
                displayCallableName(cm, mm),
                hit ? 1 : 0,
                hit ? mm.hitCount() + "x" : "never called"
            ));
        }
        for (BranchResult br : cm.branches()) {
            rows.add(new MethodFallbackPayload(
                br.conditionText() + " [T]",
                br.trueHit() ? 1 : 0,
                br.trueHit() ? "hit" : "miss"
            ));
            rows.add(new MethodFallbackPayload(
                br.conditionText() + " [F]",
                br.falseHit() ? 1 : 0,
                br.falseHit() ? "hit" : "miss"
            ));
        }
        return rows;
    }

    // -----------------------------------------------------------------------
    // Insights
    // -----------------------------------------------------------------------

    private List<InsightPayload> buildInsightPayloads(ClassMetrics cm, List<Insight> insights) {
        return insights.stream()
            .sorted(Comparator.comparingInt(Insight::line)
                .thenComparingInt(i -> i.severity().ordinal()))
            .map(i -> new InsightPayload(
                toSeverityCode(i.severity()),
                i.message(),
                insightReference(cm, i),
                i.hint(),
                Math.max(0, i.line())
            ))
            .toList();
    }

    // -----------------------------------------------------------------------
    // Invocation reports lookup
    // -----------------------------------------------------------------------

    private Map<String, InvocationReport> buildInvocationReports(String classId, ReportModel model) {
        Map<String, InvocationReport> reports = new HashMap<>();
        model.get(InvocationResult.class).ifPresent(inv -> {
            for (InvocationReport r : inv.reports()) {
                if (r.classId().equals(classId)) {
                    reports.put(MethodMetrics.methodKey(r.methodName(), r.startLine()), r);
                }
            }
        });
        return reports;
    }

    // -----------------------------------------------------------------------
    // Conversion helpers
    // -----------------------------------------------------------------------

    private static int toCoverageCode(Coverage coverage) {
        return switch (coverage) {
            case NOT_EXECUTABLE  -> CODE_NOT_EXECUTABLE;
            case HIT             -> CODE_HIT;
            case MISS            -> CODE_MISS;
            case PARTIAL_BRANCH  -> CODE_PARTIAL_BRANCH;
        };
    }

    private static int toCondCovCode(boolean allCovered, boolean anyCovered) {
        if (allCovered)  return 2; // ALL
        if (anyCovered)  return 1; // PARTIAL
        return 0;                  // NONE
    }

    private static String toSeverityCode(Severity severity) {
        return switch (severity) {
            case CRITICAL -> "C";
            case WARNING  -> "W";
            case INFO     -> "I";
            case POSITIVE -> "P";
        };
    }

    private boolean isVisibleCallable(String methodKey, Map<String, MethodMetrics> metricsByMethod) {
        MethodMetrics metrics = metricsByMethod.get(methodKey);
        return metrics == null || !metrics.isImplicitDefaultConstructor();
    }

    private String displayCallableName(ClassMetrics cm, MethodMetrics metrics) {
        if (metrics == null) {
            return "";
        }
        return metrics.isConstructor() ? cm.simpleName() + "()" : metrics.methodName();
    }

    private String insightReference(ClassMetrics cm, Insight insight) {
        if (insight.methodName() == null) {
            return insight.classId();
        }
        MethodMetrics metrics = findInsightMethod(cm, insight);
        return metrics != null ? displayCallableName(cm, metrics) : insight.methodName();
    }

    private MethodMetrics findInsightMethod(ClassMetrics cm, Insight insight) {
        return cm.methods().stream()
            .filter(mm -> mm.methodName().equals(insight.methodName()))
            .filter(mm -> insight.line() >= mm.startLine() && insight.line() <= mm.endLine())
            .min(Comparator
                .comparingInt((MethodMetrics mm) -> mm.endLine() - mm.startLine())
                .thenComparingInt(MethodMetrics::startLine))
            .orElseGet(() -> cm.methods().stream()
                .filter(mm -> mm.methodName().equals(insight.methodName()))
                .findFirst()
                .orElse(null));
    }

    private String nullIfEmpty(String s) {
        return (s == null || s.isEmpty()) ? null : s;
    }
}

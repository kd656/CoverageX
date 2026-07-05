package io.github.kd656.coveragex.core.report.views.assembler;

import io.github.kd656.coveragex.core.insights.Insight;
import io.github.kd656.coveragex.core.insights.Severity;
import io.github.kd656.coveragex.core.report.ReportScope;
import io.github.kd656.coveragex.core.report.model.ClassMetrics;
import io.github.kd656.coveragex.core.report.model.ReportModel;
import io.github.kd656.coveragex.core.report.model.SummaryMetrics;
import io.github.kd656.coveragex.core.report.pipeline.results.InsightsResult;
import io.github.kd656.coveragex.core.report.views.html.HtmlModuleNode;
import io.github.kd656.coveragex.core.report.views.html.HtmlNavNode;
import io.github.kd656.coveragex.core.report.views.html.HtmlReportViewModel;
import io.github.kd656.coveragex.core.report.views.html.HtmlSummary;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns a computed {@link ReportModel} into the {@link HtmlReportViewModel} the
 * Freemarker templates render.
 *
 * <p>Delegates the package-tree walk to {@link HtmlNavTreeBuilder}. Single-module
 * models produce a flat tree; scoped models produce one {@link HtmlModuleNode}
 * per scope, each carrying its own package tree.</p>
 */
public class HtmlViewModelAssembler {

    private final HtmlNavTreeBuilder navTreeBuilder = new HtmlNavTreeBuilder();

    public HtmlReportViewModel assemble(ReportModel model, Path sourceDirectory) {
        SummaryMetrics summary = model.getSummaryMetrics();

        List<Insight> allInsights = model.get(InsightsResult.class)
                .map(InsightsResult::insights)
                .orElse(Collections.emptyList());
        Map<String, List<Insight>> insightsByClass = groupByClassId(allInsights);
        Map<Severity, Long> countBySeverity = countBySeverity(allInsights);

        HtmlSummary topBar = buildTopBar(summary, countBySeverity);

        if (model.isScoped()) {
            List<HtmlModuleNode> modules = new ArrayList<>(model.getScopes().size());
            for (ReportScope scope : model.getScopes()) {
                modules.add(buildModuleNode(scope, insightsByClass));
            }
            return new HtmlReportViewModel(topBar, List.of(), modules);
        }

        // Single-module: flat tree at classes/<sectionId>.data.js (unchanged layout).
        List<HtmlNavNode> navTree = navTreeBuilder.build(
                model.getClassMetrics(),
                insightsByClass,
                "",
                sectionId -> "classes/" + sectionId + ".data.js");
        return new HtmlReportViewModel(topBar, navTree);
    }

    private HtmlModuleNode buildModuleNode(ReportScope scope,
                                            Map<String, List<Insight>> insightsByClass) {
        String scopeId = scope.scopeId();
        List<HtmlNavNode> tree = navTreeBuilder.build(
                scope.classMetrics(),
                insightsByClass,
                scopeId + "--",
                sectionId -> "classes/" + scopeId + "/" + sectionId + ".data.js");
        double coveragePct = averageCoverage(scope.classMetrics());
        boolean hasCrit = anyInsightIn(scope.classMetrics(), insightsByClass, Severity.CRITICAL);
        boolean hasWarn = anyInsightIn(scope.classMetrics(), insightsByClass, Severity.WARNING);
        return new HtmlModuleNode(
                scopeId,
                scope.displayName() != null ? scope.displayName() : scopeId,
                coveragePct,
                scope.classMetrics().size(),
                hasCrit,
                hasWarn,
                tree,
                true);
    }

    private HtmlSummary buildTopBar(SummaryMetrics summary, Map<Severity, Long> bySeverity) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        int notCovered = summary.totalProbes() - summary.executedProbes();
        return new HtmlSummary(
                timestamp, summary.lineCoveragePercent(),
                summary.classCount(), summary.totalProbes(), summary.executedProbes(), notCovered,
                bySeverity.getOrDefault(Severity.CRITICAL, 0L),
                bySeverity.getOrDefault(Severity.WARNING, 0L),
                bySeverity.getOrDefault(Severity.INFO, 0L),
                bySeverity.getOrDefault(Severity.POSITIVE, 0L));
    }

    private static Map<String, List<Insight>> groupByClassId(List<Insight> insights) {
        Map<String, List<Insight>> byClass = new LinkedHashMap<>();
        for (Insight insight : insights) {
            byClass.computeIfAbsent(insight.classId(), k -> new ArrayList<>()).add(insight);
        }
        return byClass;
    }

    private static Map<Severity, Long> countBySeverity(List<Insight> insights) {
        Map<Severity, Long> counts = new EnumMap<>(Severity.class);
        for (Insight insight : insights) {
            counts.merge(insight.severity(), 1L, Long::sum);
        }
        return counts;
    }

    private static double averageCoverage(List<ClassMetrics> classes) {
        if (classes.isEmpty()) return 0.0;
        double sum = 0.0;
        for (ClassMetrics cm : classes) {
            sum += cm.lineCoveragePercent();
        }
        return sum / classes.size();
    }

    private static boolean anyInsightIn(List<ClassMetrics> classes,
                                         Map<String, List<Insight>> insightsByClass,
                                         Severity severity) {
        for (ClassMetrics cm : classes) {
            List<Insight> forClass = insightsByClass.getOrDefault(cm.classId(), Collections.emptyList());
            for (Insight insight : forClass) {
                if (insight.severity() == severity) return true;
            }
        }
        return false;
    }

    /** Kept for back-compat with call sites that computed a section id externally. */
    public static String sectionId(String classId) {
        return HtmlNavTreeBuilder.sectionId(classId);
    }
}

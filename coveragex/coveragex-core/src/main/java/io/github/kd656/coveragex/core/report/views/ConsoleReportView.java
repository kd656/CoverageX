package io.github.kd656.coveragex.core.report.views;

import io.github.kd656.coveragex.api.data.InvocationRecord;
import io.github.kd656.coveragex.api.data.ProbeMetadata;
import io.github.kd656.coveragex.core.insights.Insight;
import io.github.kd656.coveragex.core.report.ProbeMetadataHintGenerator;
import io.github.kd656.coveragex.core.report.ProbeMetadataLabelFormatter;
import io.github.kd656.coveragex.core.report.ReportContext;
import io.github.kd656.coveragex.core.report.ReportView;
import io.github.kd656.coveragex.core.report.model.*;
import io.github.kd656.coveragex.core.report.pipeline.results.InsightsResult;
import io.github.kd656.coveragex.core.report.pipeline.results.InvocationReport;
import io.github.kd656.coveragex.core.report.pipeline.results.InvocationResult;
import io.github.kd656.coveragex.core.report.pipeline.results.SuggestionsResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConsoleReportView implements ReportView {

    private static final Logger LOG = LoggerFactory.getLogger(ConsoleReportView.class);
    private static final ProbeMetadataLabelFormatter LABEL_FORMATTER = new ProbeMetadataLabelFormatter();
    private static final ProbeMetadataHintGenerator HINT_GENERATOR = new ProbeMetadataHintGenerator();

    @Override
    public ReportingType type() {
        return ReportingType.CONSOLE;
    }

    @Override
    public void render(ReportModel model, ReportContext context) {
        SummaryMetrics summary = model.getSummaryMetrics();

        LOG.info("");
        LOG.info("========================================================");
        LOG.info("  CoverageX Report");
        LOG.info("========================================================");
        LOG.info("");
        LOG.info("Coverage Summary:");
        LOG.info("  Total Probes:         {}", summary.totalProbes());
        LOG.info("  Executed Probes:      {}", summary.executedProbes());
        LOG.info("  Coverage:             {}", String.format("%.2f%%", summary.lineCoveragePercent()));
        LOG.info("  Instrumented Classes: {}", summary.classCount());
        LOG.info("");

        LOG.info("Per-Class Coverage:");

        List<ClassMetrics> sortedClasses = new ArrayList<>(model.getClassMetrics());
        sortedClasses.sort(Comparator.comparing(ClassMetrics::classId));

        for (ClassMetrics cm : sortedClasses) {
            LOG.info(String.format("  %-48s  line=%.1f%%  branch=%.1f%%  method=%.1f%%",
                cm.classId(),
                cm.lineCoveragePercent(),
                cm.branchCoveragePercent(),
                cm.methodCoveragePercent()));

            // Invocation tracking (optional)
            model.get(InvocationResult.class).ifPresent(invResult -> {
                for (InvocationReport report : invResult.reports()) {
                    if (!report.classId().equals(cm.classId())) continue;
                    for (InvocationRecord rec : report.argCombinations()) {
                        LOG.info("    hit: {}({}) x{}", report.methodName(),
                            formatArgs(rec.args()), rec.count());
                    }
                }
            });

            // Not covered lines
            logNotCovered(cm);

            // Insights (optional)
            model.get(InsightsResult.class).ifPresent(insResult -> {
                List<Insight> classInsights = insResult.insights().stream()
                    .filter(i -> i.classId().equals(cm.classId()))
                    .toList();
                if (!classInsights.isEmpty()) {
                    LOG.info("  Insights:");
                    for (Insight insight : classInsights) {
                        LOG.info("    [{}] {} -- {}", insight.severity(), insight.message(), insight.hint());
                    }
                }
            });

            // Suggestions (optional)
            model.get(SuggestionsResult.class).ifPresent(sugResult -> {
                List<io.github.kd656.coveragex.core.report.pipeline.results.Suggestion> classSuggestions =
                    sugResult.suggestions().stream()
                        .filter(s -> s.classId().equals(cm.classId()))
                        .toList();
                if (!classSuggestions.isEmpty()) {
                    LOG.info("  Suggestions:");
                    for (io.github.kd656.coveragex.core.report.pipeline.results.Suggestion s : classSuggestions) {
                        LOG.info("    -> {}", s.description());
                    }
                }
            });

            LOG.info("");
        }

        LOG.info("========================================================");
        LOG.info("");
    }

    private void logNotCovered(ClassMetrics cm) {
        List<ProbeMetadata> metadata = cm.probeMetadata();
        List<LineStatus> lines = cm.lines();

        Set<Integer> missedLines = new HashSet<>();
        for (LineStatus ls : lines) {
            if (ls.coverage() == Coverage.MISS) {
                missedLines.add(ls.lineNumber());
            }
        }

        if (missedLines.isEmpty()) return;

        List<ProbeMetadata> uncoveredProbes = new ArrayList<>();
        for (ProbeMetadata pm : metadata) {
            if (pm == null) continue;
            int line = pm.lineNumber();
            if (missedLines.contains(line)) {
                uncoveredProbes.add(pm);
            }
        }

        if (uncoveredProbes.isEmpty()) return;

        uncoveredProbes.sort(Comparator.comparingInt(ProbeMetadata::lineNumber)
            .thenComparingInt(ProbeMetadata::probeId));

        LOG.info("  Not covered (sorted by line):");
        for (ProbeMetadata pm : uncoveredProbes) {
            String label = pm.accept(LABEL_FORMATTER);
            String hint = pm.accept(HINT_GENERATOR);
            LOG.info("    {}", label);
            LOG.info("      {}", hint);
            LOG.info("");
        }
    }

    private String formatArgs(List<String> args) {
        if (args == null || args.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.size(); i++) {
            if (i > 0) sb.append(", ");
            String arg = args.get(i);
            if (arg == null || "null".equals(arg) || isNumericOrBoolean(arg)) {
                sb.append(arg);
            } else {
                sb.append('"').append(arg).append('"');
            }
        }
        return sb.toString();
    }

    private boolean isNumericOrBoolean(String v) {
        if ("true".equals(v) || "false".equals(v)) return true;
        try { Long.parseLong(v); return true; } catch (NumberFormatException e1) {
            try { Double.parseDouble(v); return true; } catch (NumberFormatException e2) { return false; }
        }
    }

}

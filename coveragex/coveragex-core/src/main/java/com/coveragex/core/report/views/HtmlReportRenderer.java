package com.coveragex.core.report.views;

import com.coveragex.core.insights.Insight;
import com.coveragex.core.report.ReportContext;
import com.coveragex.core.report.ReportView;
import com.coveragex.core.report.model.ClassMetrics;
import com.coveragex.core.report.model.ReportModel;
import com.coveragex.core.report.model.ReportingType;
import com.coveragex.core.report.pipeline.results.InsightsResult;
import com.coveragex.core.report.views.assembler.ClassDataSerializer;
import com.coveragex.core.report.views.assembler.HtmlViewModelAssembler;
import com.coveragex.core.report.views.html.HtmlReportViewModel;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HtmlReportRenderer implements ReportView {

    private static final Logger LOG = LoggerFactory.getLogger(HtmlReportRenderer.class);

    private final Configuration freemarker = buildFreemarkerConfig();
    private final HtmlViewModelAssembler assembler = new HtmlViewModelAssembler();
    private final ClassDataSerializer serializer = new ClassDataSerializer();

    @Override
    public ReportingType type() { return ReportingType.HTML; }

    @Override
    public void render(ReportModel model, ReportContext context) {
        try {
            Path outDir = context.reportOutputDir();
            Path classesDir = outDir.resolve("classes");
            Files.createDirectories(classesDir);

            // Build the index.html view model (nav tree + topbar only)
            HtmlReportViewModel viewModel = assembler.assemble(model, context.sourceDirectory());

            // Write index.html
            Path htmlOut = outDir.resolve("index.html");
            Template template = freemarker.getTemplate("report.ftl");
            StringWriter out = new StringWriter();
            template.process(Map.of("report", viewModel), out);
            Files.writeString(htmlOut, out.toString(), StandardCharsets.UTF_8);
            LOG.info("CoverageX HTML report written to: {}", htmlOut);

            // Write one .data.js file per class
            List<Insight> allInsights = model.get(InsightsResult.class)
                .map(InsightsResult::insights)
                .orElse(Collections.emptyList());
            Map<String, List<Insight>> insightsByClass = allInsights.stream()
                .collect(Collectors.groupingBy(Insight::classId));

            for (ClassMetrics cm : model.getClassMetrics()) {
                writeClassDataFile(cm, insightsByClass, model, context.sourceDirectory(), classesDir);
            }

            LOG.info("CoverageX class payload files written to: {}", classesDir);
        } catch (Exception e) {
            LOG.error("Failed to write HTML report: {}", e.getMessage(), e);
        }
    }

    private void writeClassDataFile(ClassMetrics cm,
                                    Map<String, List<Insight>> insightsByClass,
                                    ReportModel model,
                                    Path sourceDirectory,
                                    Path classesDir) throws IOException {
        String sectionId = HtmlViewModelAssembler.sectionId(cm.classId());
        Path dataFile = classesDir.resolve(sectionId + ".data.js");

        Path sourceFile = resolveSourceFile(cm.classId(), sourceDirectory);
        List<Insight> classInsights = insightsByClass.getOrDefault(cm.classId(), Collections.emptyList());

        String content = serializer.serialize(cm, sourceFile, classInsights, model, sectionId);
        Files.writeString(dataFile, content, StandardCharsets.UTF_8);
    }

    private Path resolveSourceFile(String classId, Path sourceDirectory) {
        if (sourceDirectory == null) return null;
        Path candidate = sourceDirectory.resolve(classId + ".java");
        return Files.exists(candidate) ? candidate : null;
    }

    private static Configuration buildFreemarkerConfig() {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_33);
        cfg.setClassForTemplateLoading(HtmlReportRenderer.class, "/templates");
        cfg.setDefaultEncoding("UTF-8");
        cfg.setOutputEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setLogTemplateExceptions(false);
        cfg.setWrapUncheckedExceptions(true);
        return cfg;
    }
}

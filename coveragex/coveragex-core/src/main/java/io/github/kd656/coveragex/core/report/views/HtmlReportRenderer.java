package io.github.kd656.coveragex.core.report.views;

import io.github.kd656.coveragex.core.insights.Insight;
import io.github.kd656.coveragex.core.report.ReportContext;
import io.github.kd656.coveragex.core.report.ReportScope;
import io.github.kd656.coveragex.core.report.ReportRenderer;
import io.github.kd656.coveragex.core.report.model.ClassMetrics;
import io.github.kd656.coveragex.core.report.model.ReportModel;
import io.github.kd656.coveragex.core.report.model.ReportingType;
import io.github.kd656.coveragex.core.report.pipeline.results.InsightsResult;
import io.github.kd656.coveragex.core.report.views.assembler.ClassDataSerializer;
import io.github.kd656.coveragex.core.report.views.assembler.HtmlNavTreeBuilder;
import io.github.kd656.coveragex.core.report.views.assembler.HtmlViewModelAssembler;
import io.github.kd656.coveragex.core.report.views.html.HtmlReportViewModel;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HtmlReportRenderer implements ReportRenderer {

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
            Path classesRoot = outDir.resolve("classes");
            Files.createDirectories(classesRoot);

            HtmlReportViewModel viewModel = assembler.assemble(model, context.sourceDirectory());

            Path htmlOut = outDir.resolve("index.html");
            Template template = freemarker.getTemplate("report.ftl");
            StringWriter out = new StringWriter();
            template.process(Map.of("report", viewModel), out);
            Files.writeString(htmlOut, out.toString(), StandardCharsets.UTF_8);
            LOG.info("CoverageX HTML report written to: {}", htmlOut);

            Map<String, List<Insight>> insightsByClass = groupInsightsByClass(model);

            if (model.isScoped()) {
                writeScopedPayloads(model, insightsByClass, classesRoot, context.sourceDirectory());
            } else {
                writeFlatPayloads(model.getClassMetrics(), insightsByClass, model, classesRoot,
                        context.sourceDirectory(), "");
            }

            LOG.info("CoverageX class payload files written under: {}", classesRoot);
        } catch (Exception e) {
            LOG.error("Failed to write HTML report: {}", e.getMessage(), e);
        }
    }

    private void writeFlatPayloads(List<ClassMetrics> classes,
                                    Map<String, List<Insight>> insightsByClass,
                                    ReportModel model,
                                    Path classesRoot,
                                    Path sourceDirectory,
                                    String sectionIdPrefix) throws IOException {
        for (ClassMetrics cm : classes) {
            writeClassDataFile(cm, insightsByClass, model, sourceDirectory, classesRoot, sectionIdPrefix);
        }
    }

    private void writeScopedPayloads(ReportModel model,
                                      Map<String, List<Insight>> insightsByClass,
                                      Path classesRoot,
                                      Path sourceDirectory) throws IOException {
        for (ReportScope scope : model.getScopes()) {
            Path scopeDir = classesRoot.resolve(scope.scopeId());
            Files.createDirectories(scopeDir);
            // Per-scope source directory takes precedence over the top-level one; falls
            // back to the shared context source dir if the scope has none.
            Path effectiveSourceDir = scope.sourceDirectory() != null ? scope.sourceDirectory() : sourceDirectory;
            // Nav for scoped reports emits "<scopeId>--<rawSectionId>" (see
            // HtmlViewModelAssembler.buildModuleNode). The payload must register
            // under the same qualified id or the loader spinner never clears.
            String sectionIdPrefix = scope.scopeId() + "--";
            for (ClassMetrics cm : scope.classMetrics()) {
                writeClassDataFile(cm, insightsByClass, model, effectiveSourceDir, scopeDir, sectionIdPrefix);
            }
        }
    }

    private void writeClassDataFile(ClassMetrics cm,
                                     Map<String, List<Insight>> insightsByClass,
                                     ReportModel model,
                                     Path sourceDirectory,
                                     Path targetDir,
                                     String sectionIdPrefix) throws IOException {
        String rawSectionId = HtmlNavTreeBuilder.sectionId(cm.classId());
        Path dataFile = targetDir.resolve(rawSectionId + ".data.js");

        Path sourceFile = resolveSourceFile(cm.classId(), sourceDirectory);
        List<Insight> classInsights = insightsByClass.getOrDefault(cm.classId(), Collections.emptyList());

        String prefixedSectionId = sectionIdPrefix + rawSectionId;
        String content = serializer.serialize(cm, sourceFile, classInsights, model, prefixedSectionId);
        Files.writeString(dataFile, content, StandardCharsets.UTF_8);
    }

    private Path resolveSourceFile(String classId, Path sourceDirectory) {
        if (sourceDirectory == null) return null;
        Path candidate = sourceDirectory.resolve(classId + ".java");
        return Files.exists(candidate) ? candidate : null;
    }

    private static Map<String, List<Insight>> groupInsightsByClass(ReportModel model) {
        List<Insight> allInsights = model.get(InsightsResult.class)
                .map(InsightsResult::insights)
                .orElse(Collections.emptyList());
        Map<String, List<Insight>> byClass = new LinkedHashMap<>();
        for (Insight insight : allInsights) {
            byClass.computeIfAbsent(insight.classId(), k -> new ArrayList<>()).add(insight);
        }
        return byClass;
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

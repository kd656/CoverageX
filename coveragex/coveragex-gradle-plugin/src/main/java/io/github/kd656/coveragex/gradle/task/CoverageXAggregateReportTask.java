package io.github.kd656.coveragex.gradle.task;

import io.github.kd656.coveragex.core.multi.DefaultAggregateInputAssembler;
import io.github.kd656.coveragex.core.multi.GlobPatterns;
import io.github.kd656.coveragex.core.multi.ModuleCoverageDescriptor;
import io.github.kd656.coveragex.core.report.CoverageReportRunner;
import io.github.kd656.coveragex.core.report.ReportConfig;
import io.github.kd656.coveragex.core.report.ReportInput;
import io.github.kd656.coveragex.core.report.ThresholdMode;
import io.github.kd656.coveragex.core.report.ThresholdViolationException;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.nio.file.Path;
import java.util.List;

/**
 * Multi-project coverage report: assembles one input per plugin-applied subproject, renders
 * the aggregate HTML, then gates on the threshold per {@code thresholdMode} (render first, so
 * a failure still leaves a report). Registered only on the root project. Counterpart to
 * {@code coveragex:aggregate-report}.
 */
public abstract class CoverageXAggregateReportTask extends DefaultTask {

    @OutputDirectory
    public abstract DirectoryProperty getReportOutputDir();

    @Internal
    public abstract ListProperty<String> getReportFormats();

    @Internal
    public abstract ListProperty<String> getIncludes();

    @Internal
    public abstract ListProperty<String> getExcludes();

    @Internal
    public abstract ListProperty<String> getExcludeSubprojects();

    @Internal
    public abstract Property<Double> getMinimumCoverage();

    @Internal
    public abstract Property<Boolean> getFailOnLowCoverage();

    @Internal
    public abstract Property<String> getThresholdMode();

    @Internal
    public abstract Property<Boolean> getEnableInvocationTracking();

    @Internal
    public abstract Property<Boolean> getEnableInsights();

    @Internal
    public abstract Property<Boolean> getEnableSuggestions();

    @Internal
    public abstract Property<Boolean> getEnableMCDC();

    @Internal
    public abstract Property<Boolean> getEnableOverCoverageAnalysis();

    @Internal
    public abstract Property<Boolean> getSkip();

    /**
     * Participating subproject descriptors, populated at configuration time so the action
     * never crosses project boundaries at execution (config-cache compatibility).
     */
    @Internal
    public abstract ListProperty<ModuleCoverageDescriptor> getModuleDescriptors();

    @TaskAction
    public void report() {
        Logger logger = getLogger();
        if (getSkip().get()) {
            logger.lifecycle("coveragex: aggregate-report skipped.");
            return;
        }

        List<String> excludes = getExcludeSubprojects().get();
        List<ModuleCoverageDescriptor> descriptors = getModuleDescriptors().get().stream()
                .filter(d -> !GlobPatterns.matchesAny(d.scopeId(), excludes)
                        && !GlobPatterns.matchesAny(d.displayName(), excludes))
                .toList();

        DefaultAggregateInputAssembler assembler = DefaultAggregateInputAssembler.create(
                () -> descriptors, getIncludes().get(), getExcludes().get());

        List<ReportInput> inputs;
        try {
            inputs = assembler.assemble();
        } catch (Exception e) {
            throw new GradleException("coveragex: failed to assemble aggregate inputs: " + e.getMessage(), e);
        }

        if (inputs.isEmpty()) {
            throw new GradleException(
                    "coveragex: no coveragex.exec files found in any subproject — did the test tasks run?");
        }

        Path outputDir = getReportOutputDir().getAsFile().get().toPath();

        double minimumCoverage = getMinimumCoverage().get();
        ReportConfig config = ReportConfig.of(
                outputDir,
                null,
                getReportFormats().get(),
                getEnableInvocationTracking().get(),
                getEnableInsights().get(),
                getEnableSuggestions().get(),
                getEnableMCDC().get(),
                getEnableOverCoverageAnalysis().get(),
                minimumCoverage);

        // Render before gating so a threshold failure still leaves a browsable report.
        CoverageReportRunner runner = new CoverageReportRunner();
        runner.render(config, inputs);
        logger.lifecycle("coveragex: aggregate report written to {}", outputDir.resolve("index.html"));

        ThresholdMode mode = ThresholdMode.valueOf(getThresholdMode().get());
        try {
            runner.gate(inputs, minimumCoverage, mode, getFailOnLowCoverage().get());
        } catch (ThresholdViolationException e) {
            throw new GradleException(e.getMessage());
        }
    }
}

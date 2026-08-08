package io.github.kd656.coveragex.gradle.task;

import io.github.kd656.coveragex.core.multi.DefaultAggregateInputAssembler;
import io.github.kd656.coveragex.core.multi.ModuleCoverageDescriptor;
import io.github.kd656.coveragex.core.multi.Scopes;
import io.github.kd656.coveragex.core.report.CoverageReportRunner;
import io.github.kd656.coveragex.core.report.ReportConfig;
import io.github.kd656.coveragex.core.report.ReportInput;
import io.github.kd656.coveragex.core.report.ThresholdMode;
import io.github.kd656.coveragex.core.report.ThresholdViolationException;
import io.github.kd656.coveragex.gradle.internal.ClassesDirs;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Single-project coverage report. Renders the HTML, then evaluates the coverage threshold
 * and fails the build if {@code failOnLowCoverage} is on. The HTML is always fully written
 * before threshold evaluation runs, so the report is browsable even when the build fails.
 */
public abstract class CoverageXReportTask extends DefaultTask {

    @Internal
    public abstract RegularFileProperty getDestFile();

    @Internal
    public abstract RegularFileProperty getMapFile();

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getClassesDirectories();

    @OutputDirectory
    public abstract DirectoryProperty getReportOutputDir();

    @Internal
    public abstract DirectoryProperty getSourceDirectory();

    @Internal
    public abstract ListProperty<String> getReportFormats();

    @Internal
    public abstract ListProperty<String> getIncludes();

    @Internal
    public abstract ListProperty<String> getExcludes();

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

    @Internal
    public abstract Property<String> getProjectName();

    @Internal
    public abstract DirectoryProperty getProjectBaseDir();

    @TaskAction
    public void report() throws Exception {
        Logger logger = getLogger();
        if (getSkip().get()) {
            logger.lifecycle("coveragex: report skipped.");
            return;
        }

        Path dataFile = getDestFile().getAsFile().get().toPath();
        if (!Files.exists(dataFile)) {
            logger.warn("coveragex: no exec file at {} — nothing to report. Did tests run?", dataFile);
            return;
        }

        List<ReportInput> inputs = assembleInputs(dataFile);
        if (inputs.isEmpty()) {
            return;
        }

        Path outputDir = getReportOutputDir().getAsFile().get().toPath();
        Path sourceDir = getSourceDirectory().isPresent()
                ? getSourceDirectory().getAsFile().get().toPath()
                : null;

        double minimumCoverage = getMinimumCoverage().get();
        ReportConfig config = ReportConfig.of(
                outputDir,
                sourceDir,
                getReportFormats().get(),
                getEnableInvocationTracking().get(),
                getEnableInsights().get(),
                getEnableSuggestions().get(),
                getEnableMCDC().get(),
                getEnableOverCoverageAnalysis().get(),
                minimumCoverage);

        CoverageReportRunner runner = new CoverageReportRunner();
        runner.render(config, inputs);
        logger.lifecycle("coveragex: report written to {}", outputDir.resolve("index.html"));

        ThresholdMode mode = ThresholdMode.valueOf(getThresholdMode().get());
        try {
            runner.gate(inputs, minimumCoverage, mode, getFailOnLowCoverage().get());
        } catch (ThresholdViolationException e) {
            throw new GradleException(e.getMessage());
        }
    }

    private List<ReportInput> assembleInputs(Path dataFile) throws Exception {
        String name = getProjectName().get();
        Path baseDir = getProjectBaseDir().getAsFile().get().toPath().toAbsolutePath().normalize();
        Path mapFile = getMapFile().isPresent() ? getMapFile().getAsFile().get().toPath() : null;
        Path sourceDir = getSourceDirectory().isPresent()
                ? getSourceDirectory().getAsFile().get().toPath()
                : null;
        Path classesDir = firstExistingClassesDir();

        ModuleCoverageDescriptor descriptor = new ModuleCoverageDescriptor(
                Scopes.sanitize(name, "project"),
                name,
                baseDir,
                Path.of(""),
                dataFile,
                mapFile,
                sourceDir,
                classesDir);

        DefaultAggregateInputAssembler assembler = DefaultAggregateInputAssembler.create(
                () -> List.of(descriptor), getIncludes().get(), getExcludes().get());
        return assembler.assemble();
    }

    private Path firstExistingClassesDir() {
        File existing = ClassesDirs.firstExisting(getClassesDirectories().getFiles());
        return existing != null ? existing.toPath() : null;
    }
}

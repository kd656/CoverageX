package io.github.kd656.coveragex.maven;

import io.github.kd656.coveragex.core.report.ReportConfig;
import io.github.kd656.coveragex.core.report.ReportInput;
import io.github.kd656.coveragex.core.report.logic.ReportingService;
import io.github.kd656.coveragex.core.report.ThresholdEvaluation;
import io.github.kd656.coveragex.core.report.ThresholdEvaluator;
import io.github.kd656.coveragex.core.report.ThresholdMode;
import io.github.kd656.coveragex.core.report.ThresholdOutcomeReporter;
import io.github.kd656.coveragex.core.report.ThresholdViolationException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Base class shared by {@code report} and {@code aggregate-report}.
 *
 * <p>Owns every {@link Parameter @Parameter} that means the same thing on both
 * goals — one canonical {@code coveragex.*} property name per field. Subclasses
 * declare only goal-specific parameters (e.g. {@code excludeModules},
 * {@code thresholdMode}) and implement two hooks:</p>
 *
 * <ul>
 *   <li>{@link #collectInputs()} — how this goal produces its
 *       {@link ReportInput}s.</li>
 *   <li>{@link #defaultOutputDir()} — where the report lands if the user
 *       didn't set {@code coveragex.reportOutputDir}.</li>
 * </ul>
 *
 * overrides for goal-flavored defaults.</p>
 */
public abstract class AbstractCoverageReportMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    /** Name of the per-module coverage file. */
    @Parameter(property = "coveragex.destFile", defaultValue = "coveragex.exec")
    protected String destFile;

    @Parameter(property = "coveragex.minimumCoverage", defaultValue = "0")
    protected double minimumCoverage;

    @Parameter(property = "coveragex.failOnLowCoverage", defaultValue = "false")
    protected boolean failOnLowCoverage;

    @Parameter(property = "coveragex.skip", defaultValue = "false")
    protected boolean skip;

    /** Comma-separated list of report formats. Supported values: {@code html}. */
    @Parameter(property = "coveragex.reportFormats", defaultValue = "html")
    protected String reportFormats;

    @Parameter(property = "coveragex.reportOutputDir")
    protected String reportOutputDir;

    @Parameter(property = "coveragex.sourceDirectory")
    protected String sourceDirectory;

    /** Boxed so subclasses can distinguish "user set" from "use goal default". */
    @Parameter(property = "coveragex.enableInvocationTracking")
    protected Boolean enableInvocationTracking;

    @Parameter(property = "coveragex.enableInsights")
    protected Boolean enableInsights;

    @Parameter(property = "coveragex.enableSuggestions")
    protected Boolean enableSuggestions;

    @Parameter(property = "coveragex.enableMCDC")
    protected Boolean enableMCDC;

    @Parameter(property = "coveragex.enableOverCoverageAnalysis")
    protected Boolean enableOverCoverageAnalysis;

    @Parameter(property = "coveragex.thresholdMode", defaultValue = "GLOBAL")
    private ThresholdMode thresholdMode;

    @Parameter
    protected List<String> includes;

    @Parameter
    protected List<String> excludes;

    /** Produce this goal's report inputs. Empty list = nothing to do. */
    protected abstract List<ReportInput> collectInputs() throws MojoExecutionException;

    /** Fallback for {@code coveragex.reportOutputDir}. */
    protected abstract Path defaultOutputDir();

    /** How threshold gating aggregates across the produced inputs. */
    protected ThresholdMode thresholdMode() {
        return thresholdMode;
    }

    @Override
    public final void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("coveragex: report skipped.");
            return;
        }

        List<ReportInput> inputs = collectInputs();
        if (inputs.isEmpty()) {
            return;
        }

        Path outputDir = (reportOutputDir != null && !reportOutputDir.isBlank())
                ? Paths.get(reportOutputDir)
                : defaultOutputDir();
        Path sourceDir = (sourceDirectory != null && !sourceDirectory.isBlank())
                ? Paths.get(sourceDirectory)
                : null;

        boolean enableDefault = false;
        ReportConfig config = ReportConfig.of(
                outputDir,
                sourceDir,
                List.of(reportFormats.split(",")),
                orDefault(enableInvocationTracking, enableDefault),
                orDefault(enableInsights, enableDefault),
                orDefault(enableSuggestions, enableDefault),
                orDefault(enableMCDC, enableDefault),
                orDefault(enableOverCoverageAnalysis, enableDefault),
                minimumCoverage);

        new ReportingService().report(config, inputs);
        getLog().info("coveragex: report written to " + outputDir.resolve("index.html"));

        ThresholdEvaluation eval = new ThresholdEvaluator()
                .evaluate(inputs, minimumCoverage, thresholdMode());
        try {
            new ThresholdOutcomeReporter().apply(eval, failOnLowCoverage);
        } catch (ThresholdViolationException e) {
            throw new MojoFailureException(e.getMessage());
        }
    }

    protected boolean orDefault(Boolean value, boolean fallback) {
        return value != null ? value : fallback;
    }
}

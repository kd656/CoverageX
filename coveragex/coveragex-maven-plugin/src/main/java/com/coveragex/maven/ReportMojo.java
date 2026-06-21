package com.coveragex.maven;

import com.coveragex.core.report.CoverageThresholdChecker;
import com.coveragex.core.report.ReportPipelineRunner;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Generates a coverage report from CoverageX execution data.
 *
 * <p>This goal reads the binary coverage data file created during test execution
 * and generates coverage reports in the configured formats.</p>
 */
@Mojo(name = "report", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class ReportMojo extends AbstractMojo {

    /**
     * The Maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * Name of the coverage data file.
     */
    @Parameter(property = "coveragex.destFile", defaultValue = "coveragex.exec")
    private String destFile;

    /**
     * Minimum coverage percentage required (0-100).
     */
    @Parameter(property = "coveragex.minimumCoverage", defaultValue = "0")
    private double minimumCoverage;

    /**
     * Whether to fail the build if minimum coverage is not met.
     */
    @Parameter(property = "coveragex.failOnLowCoverage", defaultValue = "false")
    private boolean failOnLowCoverage;

    /**
     * Skip execution of this goal.
     */
    @Parameter(property = "coveragex.skip", defaultValue = "false")
    private boolean skip;

    /**
     * Comma-separated list of report formats to generate.
     * Supported values: {@code CONSOLE}, {@code HTML}.
     * Default is {@code console}.
     */
    @Parameter(property = "coveragex.reportFormats", defaultValue = "console")
    private String reportFormats;

    /**
     * Output directory for generated report files.
     */
    @Parameter(property = "coveragex.reportOutputDir",
               defaultValue = "${project.build.directory}/coveragex-report")
    private String reportOutputDir;

    /**
     * Directory containing the project's Java source files.
     */
    @Parameter(property = "coveragex.sourceDirectory",
               defaultValue = "${project.build.sourceDirectory}")
    private String sourceDirectory;

    @Parameter(property = "coveragex.enableInvocationTracking", defaultValue = "false")
    private boolean enableInvocationTracking;

    @Parameter(property = "coveragex.enableInsights", defaultValue = "false")
    private boolean enableInsights;

    @Parameter(property = "coveragex.enableSuggestions", defaultValue = "false")
    private boolean enableSuggestions;

    @Parameter(property = "coveragex.enableMCDC", defaultValue = "false")
    private boolean enableMCDC;

    @Parameter(property = "coveragex.enableOverCoverageAnalysis", defaultValue = "false")
    private boolean enableOverCoverageAnalysis;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Log log = getLog();

        if (skip) {
            log.info("Skipping CoverageX report generation");
            return;
        }

        Path dataFile = Paths.get(project.getBuild().getDirectory(), destFile);

        if (!Files.exists(dataFile)) {
            log.warn("");
            log.warn("========================================================");
            log.warn("  No coverage data found!");
            log.warn("========================================================");
            log.warn("");
            log.warn("Coverage data file does not exist: " + dataFile);
            log.warn("");
            log.warn("This typically means:");
            log.warn("  1. Tests were not executed");
            log.warn("  2. The prepare-agent goal was not run");
            log.warn("  3. No classes were instrumented");
            log.warn("");
            log.warn("Make sure to run tests with the CoverageX agent:");
            log.warn("  mvn clean test");
            log.warn("");
            log.warn("========================================================");
            return;
        }

        try {
            ReportPipelineRunner.Config config = new ReportPipelineRunner.Config(
                dataFile,
                List.of(reportFormats.split(",")),
                Paths.get(reportOutputDir),
                sourceDirectory != null ? Paths.get(sourceDirectory) : null,
                minimumCoverage,
                failOnLowCoverage,
                enableInvocationTracking,
                enableInsights,
                enableSuggestions,
                enableMCDC,
                enableOverCoverageAnalysis
            );

            CoverageThresholdChecker.ThresholdResult threshold = new ReportPipelineRunner().run(config);

            if (!threshold.passed()) {
                if (failOnLowCoverage) {
                    throw new MojoFailureException(String.format(
                        "Coverage %.2f%% is below minimum required %.2f%%",
                            threshold.actualCoverage(), threshold.minimumCoverage()));
                } else if (minimumCoverage > 0) {
                    log.error("Coverage below threshold (" + minimumCoverage + "%).");
                }
            } else if (minimumCoverage > 0) {
                log.info("Coverage threshold met!");
            }

        } catch (IOException e) {
            throw new MojoExecutionException("Failed to read coverage data: " + e.getMessage(), e);
        }
    }
}

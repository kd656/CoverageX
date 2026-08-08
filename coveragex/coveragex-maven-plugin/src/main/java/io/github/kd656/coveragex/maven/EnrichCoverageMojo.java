package io.github.kd656.coveragex.maven;

import io.github.kd656.coveragex.core.enrich.EnrichmentPipelineRunner;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
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
import java.util.stream.Stream;

@Mojo(name = "enrich", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class EnrichCoverageMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(property = "coveragex.destFile", defaultValue = "coveragex.exec")
    private String destFile;

    @Parameter(defaultValue = "${project.build.outputDirectory}", property = "coveragex.classesDir")
    private String classesDir;

    @Parameter(defaultValue = "${project.build.testOutputDirectory}/coveragex/coveragex.map.json",
            property = "coveragex.mapFile")
    private String mapFile;

    @Parameter(property = "coveragex.includes")
    private List<String> includes;

    @Parameter(property = "coveragex.excludes")
    private List<String> excludes;

    @Parameter(property = "coveragex.skip", defaultValue = "false")
    private boolean skip;

    @Override
    public void execute() throws MojoExecutionException {
        Log log = getLog();
        if (skip) {
            log.info("Skipping CoverageX enrichment");
            return;
        }

        Path dataFile = Paths.get(project.getBuild().getDirectory(), destFile);
        if (!Files.exists(dataFile)) {
            if (hasTestSources()) {
                log.warn("");
                log.warn("========================================================");
                log.warn("  No coverage data found for enrichment!");
                log.warn("========================================================");
                log.warn("");
                log.warn("Coverage data file does not exist: " + dataFile);
                log.warn("Run tests with coveragex:prepare-agent before coveragex:enrich.");
                log.warn("");
                log.warn("========================================================");
            } else {
                log.debug("CoverageX enrichment skipped: no test sources in "
                        + project.getArtifactId() + " and no coverage data at " + dataFile);
            }
            return;
        }

        Path classesPath = Paths.get(classesDir);
        Path testOutputPath = project.getBuild().getTestOutputDirectory() != null
                ? Paths.get(project.getBuild().getTestOutputDirectory())
                : null;
        if (testOutputPath != null && classesPath.normalize().toAbsolutePath()
                .equals(testOutputPath.normalize().toAbsolutePath())) {
            throw new MojoExecutionException(
                    "coveragex.classesDir points at test output: " + classesPath
                            + ". Configure it to production classes only.");
        }

        if (!Files.isDirectory(classesPath)) {
            log.warn("CoverageX classes directory does not exist, skipping enrichment: " + classesPath);
            return;
        }

        long started = System.nanoTime();
        try {
            Path mapPath = mapFile != null && !mapFile.isBlank() ? Paths.get(mapFile) : null;
            EnrichmentPipelineRunner.Config config = new EnrichmentPipelineRunner.Config(
                dataFile,
                List.of(classesPath),
                mapPath,
                includes != null ? includes : List.of(),
                excludes != null ? excludes : List.of()
            );

            EnrichmentPipelineRunner.Result result = new EnrichmentPipelineRunner().run(config);

            long elapsedMs = (System.nanoTime() - started) / 1_000_000;
            log.info("CoverageX enrichment complete: included " + result.includedClassCount()
                    + " production classes, added " + result.addedClassCount()
                    + " zero-coverage classes in " + elapsedMs + " ms.");
        } catch (Exception e) {
            throw new MojoExecutionException("CoverageX enrichment failed: " + e.getMessage(), e);
        }
    }

    /**
     * True when at least one of the project's declared test-compile source roots
     * contains at least one {@code .java} file.
     */
    private boolean hasTestSources() {
        List<String> testSourceRoots = project.getTestCompileSourceRoots();
        if (testSourceRoots == null) {
            return false;
        }

        for (String root : testSourceRoots) {
            Path rootPath = Paths.get(root);
            if (!Files.isDirectory(rootPath)) {
                continue;
            }

            try (Stream<Path> stream = Files.walk(rootPath)) {
                if (stream.anyMatch(p -> p.toString().endsWith(".java") && Files.isRegularFile(p))) {
                    return true;
                }
            } catch (IOException ignored) {
                // Treat unreadable trees as "no evidence of tests" — no false-positive warnings.
            }
        }
        return false;
    }
}

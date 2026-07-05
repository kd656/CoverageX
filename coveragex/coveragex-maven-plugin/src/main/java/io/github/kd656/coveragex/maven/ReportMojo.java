package io.github.kd656.coveragex.maven;

import io.github.kd656.coveragex.core.multi.CoverageArtifactPaths;
import io.github.kd656.coveragex.core.multi.DefaultAggregateInputAssembler;
import io.github.kd656.coveragex.core.multi.ModuleCoverageDescriptor;
import io.github.kd656.coveragex.core.multi.ModuleCoverageLoader;
import io.github.kd656.coveragex.core.multi.SemanticIndexLoader;
import io.github.kd656.coveragex.core.report.ReportInput;
import io.github.kd656.coveragex.core.scan.ClassCoverageFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Single-module coverage report. Runs the same discover → load → own → route
 * pipeline as {@link AggregateReportMojo}, just with a discoverer that yields
 * exactly one descriptor (this project).
 */
@Mojo(name = "report", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class ReportMojo extends AbstractCoverageReportMojo {

    @Override
    protected List<ReportInput> collectInputs() throws MojoExecutionException {
        CoverageArtifactPaths paths = MavenCoverageArtifactPaths.forProject(project, destFile);

        if (!Files.exists(paths.execFile())) {
            printMissingDataBanner(paths.execFile());
            return List.of();
        }

        Path rootDir = project.getBasedir().toPath().toAbsolutePath().normalize();
        ModuleCoverageDescriptor descriptor = MavenModuleCoverageDiscoverer
                .descriptorFor(project, rootDir, destFile);

        SemanticIndexLoader semanticIndexLoader = new SemanticIndexLoader();
        ModuleCoverageLoader loader = new ModuleCoverageLoader(semanticIndexLoader);
        ClassCoverageFilter filter = new ClassCoverageFilter(
                includes != null ? includes : List.of(),
                excludes != null ? excludes : List.of());
        DefaultAggregateInputAssembler assembler = new DefaultAggregateInputAssembler(
                () -> List.of(descriptor), loader, semanticIndexLoader, filter);

        try {
            return assembler.assemble();
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to read coverage data: " + e.getMessage(), e);
        }
    }

    @Override
    protected Path defaultOutputDir() {
        return Path.of(project.getBuild().getDirectory(), "coveragex-report");
    }

    private void printMissingDataBanner(Path dataFile) {
        getLog().warn("");
        getLog().warn("========================================================");
        getLog().warn("  No coverage data found!");
        getLog().warn("========================================================");
        getLog().warn("");
        getLog().warn("Coverage data file does not exist: " + dataFile);
        getLog().warn("");
        getLog().warn("This typically means:");
        getLog().warn("  1. Tests were not executed");
        getLog().warn("  2. The prepare-agent goal was not run");
        getLog().warn("  3. No classes were instrumented");
        getLog().warn("");
        getLog().warn("Make sure to run tests with the CoverageX agent:");
        getLog().warn("  mvn clean test");
        getLog().warn("");
        getLog().warn("========================================================");
    }
}

package io.github.kd656.coveragex.maven;

import io.github.kd656.coveragex.core.multi.DefaultAggregateInputAssembler;
import io.github.kd656.coveragex.core.multi.ModuleCoverageLoader;
import io.github.kd656.coveragex.core.multi.SemanticIndexLoader;
import io.github.kd656.coveragex.core.report.ReportInput;
import io.github.kd656.coveragex.core.scan.ClassCoverageFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Aggregates coverage across every reactor module and produces one report.
 *
 * <p>Inherited into every module: fires on each module's {@code verify} phase
 * but no-ops until the last one, so all upstream exec files are guaranteed on
 * disk before aggregation runs.</p>
 */
@Mojo(name = "aggregate-report",
        defaultPhase = LifecyclePhase.VERIFY,
        threadSafe = true)
public class AggregateReportMojo extends AbstractCoverageReportMojo {

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    @Parameter
    private List<String> excludeModules;

    @Override
    protected List<ReportInput> collectInputs() throws MojoExecutionException {
        List<MavenProject> reactor = session.getProjects();

        if (!reactor.isEmpty() && reactor.getLast() != project) {
            getLog().debug("coveragex: aggregate-report skipped — waiting for last reactor project.");
            return List.of();
        }

        MavenModuleCoverageDiscoverer discoverer = new MavenModuleCoverageDiscoverer(
                session, project, destFile, excludeModules);
        SemanticIndexLoader semanticIndexLoader = new SemanticIndexLoader();
        ModuleCoverageLoader loader = new ModuleCoverageLoader(semanticIndexLoader);
        ClassCoverageFilter filter = new ClassCoverageFilter(
                includes != null ? includes : List.of(),
                excludes != null ? excludes : List.of());
        DefaultAggregateInputAssembler assembler = new DefaultAggregateInputAssembler(
                discoverer, loader, semanticIndexLoader, filter);

        try {
            List<ReportInput> routed = assembler.assemble();
            if (routed.isEmpty()) {
                throw new MojoExecutionException(
                        "coveragex: no coveragex.exec files found in any module — did the test phase run?");
            }
            return routed;
        } catch (IOException e) {
            throw new MojoExecutionException(
                    "coveragex: failed to assemble aggregate inputs: " + e.getMessage(), e);
        }
    }

    @Override
    protected Path defaultOutputDir() {
        return Paths.get(session.getExecutionRootDirectory(), "target", "coveragex-report");
    }
}

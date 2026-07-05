package io.github.kd656.coveragex.maven;

import io.github.kd656.coveragex.core.analysis.source.SourceMapGenerator;
import io.github.kd656.coveragex.core.analysis.source.SourceMapGeneratorFactory;
import io.github.kd656.coveragex.core.multi.CombinedSemanticIndexAssembler;
import io.github.kd656.coveragex.core.multi.CoverageArtifactPaths;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Mojo(
        name = "analyze",
        defaultPhase = LifecyclePhase.TEST_COMPILE,
        threadSafe = true,
        requiresDependencyResolution = ResolutionScope.TEST
)
public class AnalyzeMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    @Parameter(property = "coveragex.includeTests", defaultValue = "false")
    private boolean includeTests;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            Log log = getLog();
            CoverageArtifactPaths paths = MavenCoverageArtifactPaths.forProject(project);

            List<Path> roots = collectSourceRoots(log);
            SourceMapGenerator generator = SourceMapGeneratorFactory.forSourceRoots(roots);
            generator.generate(roots, paths.mapFile());
            log.info("coveragex mapping written to: " + paths.mapFile());

            new CombinedSemanticIndexAssembler().assembleAndWrite(
                    paths.mapFile(),
                    upstreamMapFiles(),
                    paths.combinedMapFile());
        } catch (Exception e) {
            throw new MojoExecutionException("coveragex analyze failed", e);
        }
    }

    private List<Path> collectSourceRoots(Log log) {
        List<Path> roots = new ArrayList<>();
        for (String root : project.getCompileSourceRoots()) {
            log.info("Found a root: " + root);
            roots.add(Paths.get(root));
        }
        for (Path root : roots) {
            log.info("Scanning a root: " + root);
        }
        return roots;
    }

    /** Map file locations for every reactor project that builds strictly before this one. */
    private List<Path> upstreamMapFiles() {
        List<MavenProject> all = session.getProjects();
        int selfIndex = all.indexOf(project);
        if (selfIndex <= 0) {
            return List.of();
        }
        List<Path> paths = new ArrayList<>(selfIndex);
        for (MavenProject upstream : all.subList(0, selfIndex)) {
            paths.add(MavenCoverageArtifactPaths.forProject(upstream).mapFile());
        }
        return paths;
    }
}

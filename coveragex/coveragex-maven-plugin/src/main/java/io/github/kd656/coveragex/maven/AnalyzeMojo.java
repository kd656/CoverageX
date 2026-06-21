package io.github.kd656.coveragex.maven;

import io.github.kd656.coveragex.core.analysis.source.SourceMapGenerator;
import io.github.kd656.coveragex.core.analysis.source.SourceMapGeneratorFactory;
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

    @Parameter(defaultValue = "${project.build.directory}", readonly = true)
    private Path buildDir;

    @Parameter(property = "coveragex.includeTests", defaultValue = "false")
    private boolean includeTests;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            Log log = getLog();

            // 1) Determine source roots
            List<Path> roots = new ArrayList<>();
            for (String root : project.getCompileSourceRoots()) {
                log.info("Found a root: " + root);
                roots.add(Paths.get(root));
            }

            // 2) Emit mapping into classes output (so it is on classpath)
            Path out = buildDir.resolve("test-classes")
                    .resolve("coveragex")
                    .resolve("coveragex.map.json");

            // 3) Parse source roots and save .map.json file
            for (Path root : roots) {
                log.info("Scanning a root: " + root);
            }

            SourceMapGenerator sourceMapGenerator = SourceMapGeneratorFactory.forSourceRoots(roots);
            sourceMapGenerator.generate(roots, out);

            log.info("coveragex mapping written to: " + out);

        } catch (Exception e) {
            throw new MojoExecutionException("coveragex analyze failed", e);
        }
    }
}

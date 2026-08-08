package io.github.kd656.coveragex.maven;

import io.github.kd656.coveragex.core.multi.CoverageArtifactPaths;
import io.github.kd656.coveragex.core.multi.GlobPatterns;
import io.github.kd656.coveragex.core.multi.ModuleCoverageDescriptor;
import io.github.kd656.coveragex.core.multi.ModuleCoverageDiscoverer;
import io.github.kd656.coveragex.core.multi.Scopes;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Maven implementation of {@link ModuleCoverageDiscoverer}: walks the reactor,
 * drops pom-packaging and user-excluded modules, and builds one build-neutral
 * {@link ModuleCoverageDescriptor} per remaining project.
 */
public final class MavenModuleCoverageDiscoverer implements ModuleCoverageDiscoverer {

    private final MavenSession session;
    private final MavenProject aggregator;
    private final String execFileName;
    private final List<Pattern> excludeModulePatterns;

    public MavenModuleCoverageDiscoverer(MavenSession session,
                                          MavenProject aggregator,
                                          String destFile,
                                          List<String> excludeModules) {
        this.session = session;
        this.aggregator = aggregator;
        this.execFileName = destFile != null ? destFile : CoverageArtifactPaths.DEFAULT_EXEC_FILE_NAME;
        this.excludeModulePatterns = compilePatterns(excludeModules);
    }

    @Override
    public List<ModuleCoverageDescriptor> discover() {
        Path rootDir = aggregator.getBasedir().toPath().toAbsolutePath().normalize();
        List<ModuleCoverageDescriptor> raw = new ArrayList<>();
        for (MavenProject project : session.getProjects()) {
            if ("pom".equalsIgnoreCase(project.getPackaging())) {
                continue;
            }
            if (isExcluded(project.getArtifactId())) {
                continue;
            }
            raw.add(descriptorFor(project, rootDir, execFileName));
        }
        return Scopes.deduplicateScopeIds(raw);
    }

    static ModuleCoverageDescriptor descriptorFor(MavenProject project, Path rootDir, String execFileName) {
        Path baseDir = project.getBasedir().toPath().toAbsolutePath().normalize();
        Path relativePath = Scopes.safeRelativize(rootDir, baseDir);
        CoverageArtifactPaths paths = MavenCoverageArtifactPaths.forProject(project, execFileName);
        Path classesDir = Path.of(project.getBuild().getOutputDirectory());
        Path sourceDir = project.getBuild().getSourceDirectory() != null
                ? Path.of(project.getBuild().getSourceDirectory())
                : null;
        String displayName = project.getName() != null && !project.getName().isBlank()
                ? project.getName()
                : project.getArtifactId();
        return new ModuleCoverageDescriptor(
                sanitize(project.getArtifactId()),
                displayName,
                baseDir,
                relativePath,
                paths.execFile(),
                paths.mapFile(),
                sourceDir,
                classesDir);
    }

    /** Turns a raw artifact id into a filesystem/DOM-safe scope id. */
    static String sanitize(String raw) {
        return Scopes.sanitize(raw, "module");
    }

    private boolean isExcluded(String artifactId) {
        for (Pattern p : excludeModulePatterns) {
            if (p.matcher(artifactId).matches()) {
                return true;
            }
        }
        return false;
    }

    private List<Pattern> compilePatterns(List<String> patterns) {
        if (patterns == null) {
            return List.of();
        }
        List<Pattern> compiled = new ArrayList<>();
        for (String raw : patterns) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            compiled.add(GlobPatterns.compile(raw));
        }
        return compiled;
    }
}

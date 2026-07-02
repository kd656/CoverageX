package io.github.kd656.coveragex.maven;

import io.github.kd656.coveragex.core.multi.CoverageArtifactPaths;
import io.github.kd656.coveragex.core.multi.ModuleCoverageDescriptor;
import io.github.kd656.coveragex.core.multi.ModuleCoverageDiscoverer;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
        return assignCollisionFreeScopeIds(raw);
    }

    static ModuleCoverageDescriptor descriptorFor(MavenProject project, Path rootDir, String execFileName) {
        Path baseDir = project.getBasedir().toPath().toAbsolutePath().normalize();
        Path relativePath = safeRelativize(rootDir, baseDir);
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
        if (raw == null || raw.isBlank()) {
            return "module";
        }
        String replaced = raw.replaceAll("[^A-Za-z0-9._-]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-+|-+$", "");
        return replaced.isBlank() ? "module" : replaced;
    }

    /**
     * Appends {@code -2}, {@code -3}, ... to duplicate sanitized artifact ids.
     * Deterministic because iteration order is reactor order.
     */
    private List<ModuleCoverageDescriptor> assignCollisionFreeScopeIds(
            List<ModuleCoverageDescriptor> raw) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        List<ModuleCoverageDescriptor> assigned = new ArrayList<>(raw.size());
        for (ModuleCoverageDescriptor d : raw) {
            String base = d.scopeId();
            int seen = counts.getOrDefault(base, 0) + 1;
            counts.put(base, seen);
            String finalScopeId = seen == 1 ? base : base + "-" + seen;
            assigned.add(new ModuleCoverageDescriptor(
                    finalScopeId,
                    d.displayName(),
                    d.baseDirectory(),
                    d.relativePath(),
                    d.execFile(),
                    d.mapFile(),
                    d.sourceDirectory(),
                    d.classesDirectory()));
        }
        return assigned;
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
            compiled.add(Pattern.compile(globToRegex(raw)));
        }
        return compiled;
    }

    private static String globToRegex(String glob) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);
            if (c == '*') {
                sb.append(".*");
            } else {
                sb.append(Pattern.quote(String.valueOf(c)));
            }
        }
        return sb.toString();
    }

    private static Path safeRelativize(Path root, Path child) {
        try {
            return root.relativize(child);
        } catch (IllegalArgumentException e) {
            return child.getFileName();
        }
    }
}

package io.github.kd656.coveragex.gradle.internal;

import io.github.kd656.coveragex.core.multi.ModuleCoverageDescriptor;
import io.github.kd656.coveragex.core.multi.ModuleCoverageDiscoverer;
import io.github.kd656.coveragex.core.multi.Scopes;
import io.github.kd656.coveragex.gradle.CoverageXExtension;
import org.gradle.api.Project;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Gradle implementation of {@link ModuleCoverageDiscoverer}: walks the root project's
 * subprojects, keeps those with the CoverageX plugin applied, and produces one
 * {@link ModuleCoverageDescriptor} per subproject. Mirrors {@code MavenModuleCoverageDiscoverer}.
 */
public final class GradleModuleCoverageDiscoverer implements ModuleCoverageDiscoverer {

    public static final String COVERAGEX_PLUGIN_ID = "io.github.kd656.coveragex";

    private final Project rootProject;

    public GradleModuleCoverageDiscoverer(Project rootProject) {
        this.rootProject = rootProject;
    }

    @Override
    public List<ModuleCoverageDescriptor> discover() {
        Path rootDir = rootProject.getProjectDir().toPath().toAbsolutePath().normalize();
        List<ModuleCoverageDescriptor> raw = new ArrayList<>();
        for (Project subproject : rootProject.getSubprojects()) {
            if (!subproject.getPlugins().hasPlugin(COVERAGEX_PLUGIN_ID)) {
                continue;
            }
            raw.add(descriptorFor(subproject, rootDir));
        }
        return Scopes.deduplicateScopeIds(raw);
    }

    static ModuleCoverageDescriptor descriptorFor(Project project, Path rootDir) {
        CoverageXExtension extension = project.getExtensions().getByType(CoverageXExtension.class);

        Path baseDir = project.getProjectDir().toPath().toAbsolutePath().normalize();
        Path relativePath = Scopes.safeRelativize(rootDir, baseDir);
        Path execFile = extension.getDestFile().getAsFile().get().toPath();
        Path mapFile = extension.getMapFile().getAsFile().get().toPath();
        Path sourceDir = extension.getSourceDirectory().isPresent()
                ? extension.getSourceDirectory().getAsFile().get().toPath()
                : null;
        Path classesDir = firstExistingClassesDir(project, extension);
        String displayName = project.getName();

        return new ModuleCoverageDescriptor(
                Scopes.sanitize(project.getName(), "module"),
                displayName,
                baseDir,
                relativePath,
                execFile,
                mapFile,
                sourceDir,
                classesDir);
    }

    private static Path firstExistingClassesDir(Project project, CoverageXExtension extension) {
        File existing = ClassesDirs.firstExisting(extension.getClassesDirectories().getFiles());
        if (existing != null) {
            return existing.toPath();
        }
        return project.getLayout().getBuildDirectory().dir("classes/java/main")
                .get().getAsFile().toPath();
    }
}

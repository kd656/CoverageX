package io.github.kd656.coveragex.maven;

import io.github.kd656.coveragex.core.multi.CoverageArtifactPaths;
import org.apache.maven.project.MavenProject;

import java.nio.file.Path;

/**
 * Maven-side factory that turns a {@link MavenProject} into the build-neutral
 * {@link CoverageArtifactPaths}.
 */
final class MavenCoverageArtifactPaths {

    private MavenCoverageArtifactPaths() {}

    static CoverageArtifactPaths forProject(MavenProject project) {
        return new CoverageArtifactPaths(
                Path.of(project.getBuild().getDirectory()),
                Path.of(project.getBuild().getTestOutputDirectory()));
    }

    static CoverageArtifactPaths forProject(MavenProject project, String execFileName) {
        return new CoverageArtifactPaths(
                Path.of(project.getBuild().getDirectory()),
                Path.of(project.getBuild().getTestOutputDirectory()),
                execFileName);
    }
}

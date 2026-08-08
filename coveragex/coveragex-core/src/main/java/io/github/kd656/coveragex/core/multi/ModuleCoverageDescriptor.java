package io.github.kd656.coveragex.core.multi;

import java.nio.file.Path;

/**
 * Per-module metadata produced by a {@code ModuleCoverageDiscoverer} implementation.
 *
 * <p>Holds identity ({@code scopeId}, {@code displayName}) and paths only — no build-tool
 * types leak through. The Maven discoverer builds these from {@code MavenProject}; a
 * future Gradle discoverer builds them from a resolved {@code Configuration}. Downstream
 * aggregation code depends only on this record, not on any build-tool API.</p>
 *
 * @param scopeId          sanitized, collision-suffixed identifier used in filesystem
 *                         paths, DOM ids, and URLs
 * @param displayName      human-readable name for diagnostics (typically the artifact
 *                         name)
 * @param baseDirectory    project base directory
 * @param relativePath     path relative to the build root; combined with
 *                         {@code displayName} it disambiguates otherwise-identical
 *                         projects in error messages
 * @param execFile         expected location of {@code coveragex.exec}
 * @param mapFile          expected location of {@code coveragex.map.json}
 * @param sourceDirectory  main source directory (used by the report to render source
 *                         listings)
 * @param classesDirectory compiled production classes directory (used to detect
 *                         DTO-only modules whose exec file is absent)
 */
public record ModuleCoverageDescriptor(
        String scopeId,
        String displayName,
        Path baseDirectory,
        Path relativePath,
        Path execFile,
        Path mapFile,
        Path sourceDirectory,
        Path classesDirectory
) {}

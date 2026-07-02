package io.github.kd656.coveragex.core.multi;

import java.nio.file.Path;

/**
 * Single source of truth for the artifact layout every mojo and adapter reads
 * or writes.
 */
public record CoverageArtifactPaths(
        Path buildDir,
        Path testOutputDir,
        String execFileName) {

    public static final String DEFAULT_EXEC_FILE_NAME = "coveragex.exec";
    public static final String MAP_FILE_NAME = "coveragex.map.json";
    public static final String COMBINED_MAP_FILE_NAME = "coveragex.map.combined.json";
    private static final String MAP_SUBDIR = "coveragex";

    public CoverageArtifactPaths(Path buildDir, Path testOutputDir) {
        this(buildDir, testOutputDir, DEFAULT_EXEC_FILE_NAME);
    }

    /** {@code target/coveragex.exec} (or user-configured {@code execFileName}). */
    public Path execFile() {
        return buildDir.resolve(execFileName);
    }

    /**
     * {@code target/test-classes/coveragex/coveragex.map.json} — the per-module
     * semantic index written by {@code AnalyzeMojo}.
     */
    public Path mapFile() {
        return testOutputDir.resolve(MAP_SUBDIR).resolve(MAP_FILE_NAME);
    }

    /**
     * {@code target/coveragex.map.combined.json} — the map the agent reads at
     * instrumentation time, unioning the local module with every upstream
     * module.
     */
    public Path combinedMapFile() {
        return buildDir.resolve(COMBINED_MAP_FILE_NAME);
    }
}

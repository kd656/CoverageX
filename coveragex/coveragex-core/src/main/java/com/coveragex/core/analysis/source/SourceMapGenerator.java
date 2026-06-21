package com.coveragex.core.analysis.source;

import java.nio.file.Path;
import java.util.List;

/**
 * Parses source files and writes a {@code coveragex.map.json} file describing every class,
 * method, and branch decision found in the given source roots.
 * <p>Implementations are language-specific (e.g. {@link JavaSourceMapGenerator}).
 * Use {@link SourceMapGeneratorFactory} to obtain the right implementation for a project.</p>
 */
public interface SourceMapGenerator {
    void generate(List<Path> sourceRoots, Path outputPath) throws Exception;
}

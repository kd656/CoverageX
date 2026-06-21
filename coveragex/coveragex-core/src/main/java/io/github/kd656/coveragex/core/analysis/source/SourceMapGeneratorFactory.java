package io.github.kd656.coveragex.core.analysis.source;

import java.nio.file.Path;
import java.util.List;

/**
 * Selects the appropriate {@link SourceMapGenerator} implementation based on the languages
 * detected in the given source roots.
 * <p>Currently always returns {@link JavaSourceMapGenerator}. Kotlin detection will be added
 * when {@code KotlinSourceMapGenerator} is implemented.</p>
 */
public final class SourceMapGeneratorFactory {
    private SourceMapGeneratorFactory() {}

    public static SourceMapGenerator forSourceRoots(List<Path> sourceRoots) {
        // TODO: detect Kotlin (.kt files) when KotlinSourceMapGenerator is implemented
        return new JavaSourceMapGenerator();
    }
}

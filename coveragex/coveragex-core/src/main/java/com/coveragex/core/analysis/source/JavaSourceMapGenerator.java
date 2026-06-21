package com.coveragex.core.analysis.source;

import com.coveragex.core.analysis.source.impl.SourceCodeAnalyzer;
import com.coveragex.core.analysis.source.model.SemanticIndex;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Java implementation of {@link SourceMapGenerator} backed by JavaParser.
 * <p>Walks {@code .java} source files, extracts methods and branch decisions,
 * and serialises the result to {@code coveragex.map.json} via Jackson.</p>
 */
final class JavaSourceMapGenerator implements SourceMapGenerator {

    @Override
    public void generate(List<Path> sourceRoots, Path outputPath) throws Exception {
        Objects.requireNonNull(sourceRoots, "sourceRoots must not be null");
        Objects.requireNonNull(outputPath, "outputPath must not be null");

        if (sourceRoots.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("sourceRoots must not contain null elements");
        }

        ParserConfiguration cfg = new ParserConfiguration()
                .setAttributeComments(false)
                .setStoreTokens(true);

        JavaParser parser = new JavaParser(cfg);
        SemanticIndex index = new SemanticIndex();
        SourceAnalyzer analyzer = new SourceCodeAnalyzer(parser, index);

        for (Path root : sourceRoots) {
            analyzer.scan(root);
        }

        new JacksonCoverageContextLoader().save(outputPath, index);
    }
}

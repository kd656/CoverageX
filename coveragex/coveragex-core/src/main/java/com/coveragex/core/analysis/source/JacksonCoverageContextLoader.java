package com.coveragex.core.analysis.source;

import com.coveragex.core.analysis.source.model.SemanticIndex;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class JacksonCoverageContextLoader implements CoverageContextLoader<SemanticIndex> {

    private final ObjectMapper mapper;

    private SemanticIndex cached;

    public JacksonCoverageContextLoader() {
        this.mapper = new ObjectMapper();
    }

    @Override
    public synchronized SemanticIndex load(Path path) throws IOException {
        if (Objects.nonNull(cached)) {
            return cached;
        }

        if (path == null || !Files.exists(path)) {
            throw new IOException("File was not found.");
        }

        try {
            SemanticIndex semanticIndex = mapper.readValue(path.toFile(), SemanticIndex.class);
            this.cached = semanticIndex;

            return semanticIndex;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load SemanticIndex from " + path, e);
        }
    }

    @Override
    public synchronized void save(Path path, SemanticIndex model) throws IOException {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        mapper.writeValue(path.toFile(), model);
    }
}

package io.github.kd656.coveragex.core.multi;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kd656.coveragex.core.analysis.source.model.SemanticIndex;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Reader for the {@code coveragex.map.json} file that {@code AnalyzeMojo} writes
 * at {@code TEST_COMPILE}. Memoizes per-path so the aggregate mojo can read the
 * same module map twice (ownership build + zero synthesis) without re-parsing.
 *
 * <p>Not thread-safe; one instance per mojo {@code execute()} call.</p>
 */
public class SemanticIndexLoader {

    private final ObjectMapper mapper;
    private final Map<Path, SemanticIndex> cache = new HashMap<>();

    public SemanticIndexLoader() {
        this(new ObjectMapper());
    }

    public SemanticIndexLoader(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public SemanticIndex load(Path mapFile) throws IOException {
        if (mapFile == null || !Files.exists(mapFile)) {
            throw new NoSuchFileException(String.valueOf(mapFile));
        }
        SemanticIndex cached = cache.get(mapFile);
        if (cached != null) {
            return cached;
        }
        SemanticIndex loaded = mapper.readValue(mapFile.toFile(), SemanticIndex.class);
        cache.put(mapFile, loaded);
        return loaded;
    }
}

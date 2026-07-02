package io.github.kd656.coveragex.core.multi;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kd656.coveragex.core.analysis.source.model.SemanticIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Combines a local {@link SemanticIndex} with any number of upstream ones and
 * writes the result. The agent reads the combined map at instrumentation time,
 * so a class loaded through a transitive dependency still resolves to its
 * source-derived {@code conditionText} instead of an opcode fallback.
 */
public final class CombinedSemanticIndexAssembler {

    private static final Logger LOG = LoggerFactory.getLogger(CombinedSemanticIndexAssembler.class);

    private final SemanticIndexLoader loader;
    private final ObjectMapper mapper;

    public CombinedSemanticIndexAssembler() {
        this(new SemanticIndexLoader(), new ObjectMapper());
    }

    public CombinedSemanticIndexAssembler(SemanticIndexLoader loader, ObjectMapper mapper) {
        this.loader = loader;
        this.mapper = mapper;
    }

    /** Assemble + write in one call. Missing upstream maps are silently skipped. */
    public void assembleAndWrite(Path localMap, List<Path> upstreamMaps, Path destination) throws IOException {
        SemanticIndex combined = loader.load(localMap);
        int foldedIn = 0;
        for (Path upstreamMap : upstreamMaps) {
            if (!Files.isRegularFile(upstreamMap)) {
                LOG.debug("coveragex: skipping missing upstream map {}", upstreamMap);
                continue;
            }
            SemanticIndexMerger.mergePreservingFirst(combined, loader.load(upstreamMap));
            foldedIn++;
        }
        Files.createDirectories(destination.getParent());
        mapper.writeValue(destination.toFile(), combined);
        if (foldedIn > 0) {
            LOG.info("coveragex combined mapping written to {} (local + {} upstream module(s))",
                    destination, foldedIn);
        } else {
            LOG.debug("coveragex combined mapping written to {} (local only)", destination);
        }
    }
}

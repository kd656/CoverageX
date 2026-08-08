package io.github.kd656.coveragex.core.multi;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kd656.coveragex.core.analysis.source.model.SemanticIndex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SemanticIndexLoaderTest {

    @Test
    void loadDeserializesJsonWrittenByMapper(@TempDir Path tmp) throws Exception {
        SemanticIndex written = new SemanticIndex();
        written.getOrCreateClass("com/example/Foo", "Foo.java");
        written.getOrCreateClass("com/example/Bar", "Bar.java");

        Path file = tmp.resolve("coveragex.map.json");
        new ObjectMapper().writeValue(file.toFile(), written);

        SemanticIndex loaded = new SemanticIndexLoader().load(file);

        assertThat(loaded.getClasses().keySet())
            .containsExactlyInAnyOrder("com/example/Foo", "com/example/Bar");
    }

    @Test
    void loadThrowsWhenFileMissing(@TempDir Path tmp) {
        Path missing = tmp.resolve("does-not-exist.json");
        assertThatThrownBy(() -> new SemanticIndexLoader().load(missing))
            .isInstanceOf(NoSuchFileException.class);
    }

    @Test
    void loadThrowsWhenPathIsNull() {
        assertThatThrownBy(() -> new SemanticIndexLoader().load(null))
            .isInstanceOf(NoSuchFileException.class);
    }
}

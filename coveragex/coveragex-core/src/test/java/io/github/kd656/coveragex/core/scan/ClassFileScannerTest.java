package io.github.kd656.coveragex.core.scan;

import io.github.kd656.coveragex.core.analysis.source.CoverageContextResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClassFileScannerTest {

    @TempDir
    Path tempDir;

    @Test
    void readsClassIdentityFromBytecodeInsteadOfPath() throws IOException {
        Path misleadingPath = tempDir.resolve("org/example/deep/WrongName.class");
        Files.createDirectories(misleadingPath.getParent());
        Files.write(misleadingPath, classBytes(Fixture.class));

        String actualClassId = Fixture.class.getName().replace('.', '/');
        ClassCoverageFilter filter = new ClassCoverageFilter(List.of("io.github.kd656.coveragex.**"), null);

        var plans = new ClassFileScanner().scan(tempDir, filter, new CoverageContextResolver(null));

        assertThat(plans).containsOnlyKeys(actualClassId);
        assertThat(plans.get(actualClassId).probeCount()).isGreaterThan(0);
    }

    private static byte[] classBytes(Class<?> type) throws IOException {
        String resource = "/" + type.getName().replace('.', '/') + ".class";
        try (var input = type.getResourceAsStream(resource)) {
            assertThat(input).isNotNull();
            return input.readAllBytes();
        }
    }

    static class Fixture {
        String choose(boolean flag) {
            if (flag) {
                return "yes";
            }
            return "no";
        }
    }
}

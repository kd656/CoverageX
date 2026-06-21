package io.github.kd656.coveragex.maven;

import io.github.kd656.coveragex.api.data.ClassCoverage;
import io.github.kd656.coveragex.api.data.ExecutionData;
import io.github.kd656.coveragex.core.analysis.source.CoverageContextResolver;
import io.github.kd656.coveragex.core.enrich.CoverageDataEnrichmentService;
import io.github.kd656.coveragex.core.scan.ClassCoverageFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CoverageDataEnrichmentServiceTest {

    @TempDir
    Path classesDir;

    @Test
    void addsZeroCoverageForMissingProductionClassesAndPreservesRuntimeData() throws IOException {
        Path classFile = classesDir.resolve("misleading/path/Fixture.class");
        Files.createDirectories(classFile.getParent());
        Files.write(classFile, classBytes(Fixture.class));

        String fixtureClassId = Fixture.class.getName().replace('.', '/');
        String loadedClassId = "org/example/Loaded";
        ClassCoverage loadedCoverage = new ClassCoverage(
                loadedClassId,
                new boolean[] {true},
                Map.of(),
                Map.of(),
                List.of(),
                null
        );
        Map<String, ClassCoverage> runtimeClasses = new LinkedHashMap<>();
        runtimeClasses.put(loadedClassId, loadedCoverage);

        CoverageDataEnrichmentService.EnrichmentResult result = new CoverageDataEnrichmentService().enrich(
                new ExecutionData(runtimeClasses),
                classesDir,
                new ClassCoverageFilter(List.of("io.github.kd656.coveragex.**"), null),
                new CoverageContextResolver(null)
        );

        assertThat(result.addedClassCount()).isEqualTo(1);
        assertThat(result.executionData().classCoverage(loadedClassId)).isSameAs(loadedCoverage);
        ClassCoverage zeroCoverage = result.executionData().classCoverage(fixtureClassId);
        assertThat(zeroCoverage).isNotNull();
        assertThat(zeroCoverage.probeHits()).containsOnly(false);
        assertThat(zeroCoverage.probeMetadata()).hasSize(zeroCoverage.probeHits().length);
    }

    private static byte[] classBytes(Class<?> type) throws IOException {
        String resource = "/" + type.getName().replace('.', '/') + ".class";
        try (var input = type.getResourceAsStream(resource)) {
            assertThat(input).isNotNull();
            return input.readAllBytes();
        }
    }

    static class Fixture {
        boolean check(int value) {
            return value == 42;
        }
    }
}

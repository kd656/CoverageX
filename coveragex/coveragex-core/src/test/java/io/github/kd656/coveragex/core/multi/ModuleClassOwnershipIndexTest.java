package io.github.kd656.coveragex.core.multi;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kd656.coveragex.core.analysis.source.model.SemanticIndex;
import io.github.kd656.coveragex.core.scan.ClassCoverageFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ModuleClassOwnershipIndexTest {

    @Test
    void firstBuildOrderOccurrenceWinsOnCollision(@TempDir Path tmp) throws IOException {
        ModuleCoverageDescriptor dto = descriptor(tmp, "dto",
            List.of("com/example/dto/UserRecord"));
        ModuleCoverageDescriptor service = descriptor(tmp, "service",
            List.of("com/example/service/UserService", "com/example/dto/UserRecord"));

        ModuleClassOwnershipIndex ownership = ModuleClassOwnershipIndex.build(
            List.of(dto, service),
            null,
            new SemanticIndexLoader());

        assertThat(ownership.ownerOf("com/example/dto/UserRecord")).contains("dto");
        assertThat(ownership.ownerOf("com/example/service/UserService")).contains("service");
    }

    @Test
    void unknownClassHasNoOwner(@TempDir Path tmp) throws IOException {
        ModuleCoverageDescriptor dto = descriptor(tmp, "dto",
            List.of("com/example/dto/UserRecord"));

        ModuleClassOwnershipIndex ownership = ModuleClassOwnershipIndex.build(
            List.of(dto),
            null,
            new SemanticIndexLoader());

        assertThat(ownership.ownerOf("com/example/nowhere/Foo")).isEmpty();
    }

    @Test
    void descriptorsWithoutMapFileAreSkipped(@TempDir Path tmp) throws IOException {
        ModuleCoverageDescriptor noMap = new ModuleCoverageDescriptor(
            "empty", "empty",
            tmp, Path.of("empty"),
            null, null, null, null);
        ModuleCoverageDescriptor dto = descriptor(tmp, "dto",
            List.of("com/example/dto/UserRecord"));

        ModuleClassOwnershipIndex ownership = ModuleClassOwnershipIndex.build(
            List.of(noMap, dto),
            null,
            new SemanticIndexLoader());

        assertThat(ownership.size()).isEqualTo(1);
        assertThat(ownership.ownerOf("com/example/dto/UserRecord")).contains("dto");
    }

    @Test
    void aggregateFilterExcludesMatchingClasses(@TempDir Path tmp) throws IOException {
        ModuleCoverageDescriptor dto = descriptor(tmp, "dto",
            List.of("com/example/dto/UserRecord", "com/example/generated/Proto"));

        ClassCoverageFilter filter = new ClassCoverageFilter(
            List.of("com.example.**"),
            List.of("com.example.generated.**"));

        ModuleClassOwnershipIndex ownership = ModuleClassOwnershipIndex.build(
            List.of(dto),
            filter,
            new SemanticIndexLoader());

        assertThat(ownership.ownerOf("com/example/dto/UserRecord")).contains("dto");
        assertThat(ownership.ownerOf("com/example/generated/Proto")).isEmpty();
    }

    @Test
    void emptyIndexHasNoOwnersAndSizeZero() {
        ModuleClassOwnershipIndex ownership = ModuleClassOwnershipIndex.empty();
        assertThat(ownership.size()).isZero();
        assertThat(ownership.knownClasses()).isEmpty();
        assertThat(ownership.ownerOf("anything")).isEmpty();
    }

    private static ModuleCoverageDescriptor descriptor(Path tmp, String scopeId,
                                                        List<String> classNames) throws IOException {
        Path mapFile = tmp.resolve(scopeId + "-coveragex.map.json");
        SemanticIndex index = new SemanticIndex();
        for (String className : classNames) {
            String sourceFile = className.substring(className.lastIndexOf('/') + 1) + ".java";
            index.getOrCreateClass(className, sourceFile);
        }
        new ObjectMapper().writeValue(mapFile.toFile(), index);
        return new ModuleCoverageDescriptor(
            scopeId, scopeId,
            tmp, Path.of(scopeId),
            null, mapFile, null, null);
    }
}

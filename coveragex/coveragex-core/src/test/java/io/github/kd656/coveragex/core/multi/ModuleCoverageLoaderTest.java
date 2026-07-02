package io.github.kd656.coveragex.core.multi;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kd656.coveragex.api.data.ClassCoverage;
import io.github.kd656.coveragex.api.data.ExecutionData;
import io.github.kd656.coveragex.api.io.internal.BinaryDataWriter;
import io.github.kd656.coveragex.core.analysis.source.model.SemanticIndex;
import io.github.kd656.coveragex.core.report.ReportInput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ModuleCoverageLoaderTest {

    private final ModuleCoverageLoader loader = new ModuleCoverageLoader(new SemanticIndexLoader());

    @Test
    void deserializesExecFile(@TempDir Path tmp) throws IOException {
        Path execFile = tmp.resolve("service.exec");
        writeExec(execFile, "com/example/service/UserService");

        List<ReportInput> inputs = loader.load(List.of(descriptor(
                "service", tmp, execFile, /* mapFile */ null, /* classes */ null)));

        assertThat(inputs).hasSize(1);
        assertThat(inputs.getFirst().executionData().classes())
                .containsOnlyKeys("com/example/service/UserService");
    }

    @Test
    void synthesizesZeroCoverageWhenClassesPresentButNoExec(@TempDir Path tmp) throws IOException {
        Path classesDir = Files.createDirectories(tmp.resolve("dto-classes"));
        Path mapFile = tmp.resolve("dto.map.json");
        writeSemanticIndex(mapFile, List.of("com/example/dto/UserRecord", "com/example/dto/AddressDto"));

        List<ReportInput> inputs = loader.load(List.of(descriptor(
                "dto", tmp, tmp.resolve("dto-nonexistent.exec"), mapFile, classesDir)));

        assertThat(inputs).hasSize(1);
        assertThat(inputs.getFirst().executionData().classes().keySet())
                .containsExactlyInAnyOrder("com/example/dto/UserRecord", "com/example/dto/AddressDto");
        for (ClassCoverage cc : inputs.getFirst().executionData().classes().values()) {
            assertThat(cc.probeHits()).isEmpty();
        }
    }

    @Test
    void skipsDescriptorWithNoExecAndNoClasses(@TempDir Path tmp) throws IOException {
        List<ReportInput> inputs = loader.load(List.of(descriptor(
                "empty", tmp, tmp.resolve("nothing.exec"), /* mapFile */ null, /* classes */ null)));

        assertThat(inputs).isEmpty();
    }

    @Test
    void handlesMixedDescriptors(@TempDir Path tmp) throws IOException {
        Path serviceExec = tmp.resolve("service.exec");
        writeExec(serviceExec, "com/example/service/UserService");

        Path dtoClasses = Files.createDirectories(tmp.resolve("dto-classes"));
        Path dtoMap = tmp.resolve("dto.map.json");
        writeSemanticIndex(dtoMap, List.of("com/example/dto/UserRecord"));

        List<ReportInput> inputs = loader.load(List.of(
                descriptor("service", tmp, serviceExec, null, null),
                descriptor("dto",     tmp, tmp.resolve("dto.exec"), dtoMap, dtoClasses),
                descriptor("empty",   tmp, tmp.resolve("empty.exec"), null, null)));

        assertThat(inputs)
                .extracting(ReportInput::scopeId)
                .containsExactly("service", "dto");
    }

    private static ModuleCoverageDescriptor descriptor(String scopeId, Path baseDir,
                                                        Path execFile, Path mapFile, Path classesDir) {
        return new ModuleCoverageDescriptor(
                scopeId, scopeId, baseDir, Path.of(scopeId),
                execFile, mapFile, null, classesDir);
    }

    private static void writeExec(Path file, String classId) throws IOException {
        ClassCoverage cc = new ClassCoverage(classId, new boolean[]{true},
                Map.of(), Map.of(), List.of(), null);
        ExecutionData data = new ExecutionData(Map.of(classId, cc));
        new BinaryDataWriter().write(file, data);
    }

    private static void writeSemanticIndex(Path file, List<String> classNames) throws IOException {
        SemanticIndex index = new SemanticIndex();
        for (String className : classNames) {
            String source = className.substring(className.lastIndexOf('/') + 1) + ".java";
            index.getOrCreateClass(className, source);
        }
        new ObjectMapper().writeValue(file.toFile(), index);
    }
}

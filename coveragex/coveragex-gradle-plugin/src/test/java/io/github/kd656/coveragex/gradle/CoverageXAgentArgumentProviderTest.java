package io.github.kd656.coveragex.gradle;

import io.github.kd656.coveragex.gradle.internal.CoverageXAgentArgumentProvider;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CoverageXAgentArgumentProviderTest {

    @TempDir Path tempDir;

    private Project project;
    private File agentJar;
    private File destFile;
    private File mapFile;

    @BeforeEach
    void setUp() throws IOException {
        project = ProjectBuilder.builder().build();
        agentJar = Files.createFile(tempDir.resolve("coveragex-agent-0.1.0-SNAPSHOT.jar")).toFile();
        destFile = Files.createFile(tempDir.resolve("coveragex.exec")).toFile();
        mapFile = Files.createFile(tempDir.resolve("coveragex.map.json")).toFile();
    }

    @Test
    void producesJavaagentArgumentWithBuilderSyntax() {
        CoverageXAgentArgumentProvider provider = newProvider(false, List.of("com.example.*"), List.of("**.Test"));

        List<String> args = toList(provider.asArguments());

        assertThat(args).hasSize(1);
        String arg = args.get(0);
        assertThat(arg).startsWith("-javaagent:" + agentJar.getAbsolutePath() + "=");
        assertThat(arg).contains("destfile=" + destFile.getAbsolutePath());
        assertThat(arg).contains("mapfile=" + mapFile.getAbsolutePath());
        assertThat(arg).contains("include=com.example.*");
        assertThat(arg).contains("exclude=**.Test");
    }

    @Test
    void emitsNoArgumentsWhenSkipped() {
        CoverageXAgentArgumentProvider provider = newProvider(true, List.of(), List.of());
        assertThat(provider.asArguments()).isEmpty();
    }

    private CoverageXAgentArgumentProvider newProvider(boolean skip,
                                                       List<String> includes, List<String> excludes) {
        CoverageXAgentArgumentProvider provider = project.getObjects()
                .newInstance(CoverageXAgentArgumentProvider.class);
        provider.getAgentClasspath().from(agentJar);
        provider.getDestFile().set(destFile);
        provider.getMapFile().set(mapFile);
        provider.getIncludes().set(includes);
        provider.getExcludes().set(excludes);
        provider.getSkip().set(skip);
        return provider;
    }

    private static List<String> toList(Iterable<String> it) {
        return java.util.stream.StreamSupport.stream(it.spliterator(), false).toList();
    }
}

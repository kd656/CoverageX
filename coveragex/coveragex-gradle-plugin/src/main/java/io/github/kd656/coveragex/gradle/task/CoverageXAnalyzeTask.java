package io.github.kd656.coveragex.gradle.task;

import io.github.kd656.coveragex.core.analysis.source.SourceMapGenerator;
import io.github.kd656.coveragex.core.analysis.source.SourceMapGeneratorFactory;
import io.github.kd656.coveragex.core.multi.CombinedSemanticIndexAssembler;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates the source-aware map ({@code coveragex.map.json}) from the project's main source,
 * then merges in the maps of upstream project dependencies to produce the combined map
 * ({@code coveragex.map.combined.json}) the agent reads. Counterpart to {@code coveragex:analyze}.
 */
public abstract class CoverageXAnalyzeTask extends DefaultTask {

    /** {@code @Internal} because DTO-only modules may have no {@code src/main/java}; the action checks. */
    @Internal
    public abstract DirectoryProperty getSourceDirectory();

    @OutputFile
    public abstract RegularFileProperty getMapFile();

    /** Combined maps of upstream project dependencies; empty when there are none. */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getUpstreamMapFiles();

    /** The map the agent reads: this module unioned with every upstream map. */
    @OutputFile
    public abstract RegularFileProperty getCombinedMapFile();

    @Internal
    public abstract Property<Boolean> getSkip();

    @TaskAction
    public void analyze() throws Exception {
        if (getSkip().get()) {
            getLogger().lifecycle("coveragex: analyze skipped.");
            return;
        }

        Path sourceRoot = getSourceDirectory().getAsFile().get().toPath();
        if (!Files.isDirectory(sourceRoot)) {
            getLogger().lifecycle("coveragex: no source directory at {} — nothing to analyze.", sourceRoot);
            return;
        }
        Path mapPath = getMapFile().getAsFile().get().toPath();

        List<Path> roots = List.of(sourceRoot);
        SourceMapGenerator generator = SourceMapGeneratorFactory.forSourceRoots(roots);
        generator.generate(roots, mapPath);
        getLogger().lifecycle("coveragex: mapping written to {}", mapPath);

        List<Path> upstreamMaps = new ArrayList<>();
        for (File file : getUpstreamMapFiles().getFiles()) {
            upstreamMaps.add(file.toPath());
        }
        Path combinedPath = getCombinedMapFile().getAsFile().get().toPath();
        new CombinedSemanticIndexAssembler().assembleAndWrite(mapPath, upstreamMaps, combinedPath);
    }
}

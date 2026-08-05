package io.github.kd656.coveragex.gradle.task;

import io.github.kd656.coveragex.core.enrich.EnrichmentPipelineRunner;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Enriches {@code coveragex.exec} with zero-coverage entries for production classes that
 * were never loaded during tests. Counterpart to {@code coveragex:enrich}.
 */
public abstract class CoverageXEnrichTask extends DefaultTask {

    @Internal
    public abstract RegularFileProperty getDestFile();

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getClassesDirectories();

    @Internal
    public abstract RegularFileProperty getMapFile();

    @Internal
    public abstract ListProperty<String> getIncludes();

    @Internal
    public abstract ListProperty<String> getExcludes();

    @Internal
    public abstract Property<Boolean> getSkip();

    @TaskAction
    public void enrich() throws Exception {
        if (getSkip().get()) {
            getLogger().lifecycle("coveragex: enrich skipped.");
            return;
        }

        Path dataFile = getDestFile().getAsFile().get().toPath();
        if (!Files.exists(dataFile)) {
            getLogger().lifecycle("coveragex: no exec file at {} — enrich has nothing to do.", dataFile);
            return;
        }

        List<Path> classesPaths = new ArrayList<>();
        for (File dir : getClassesDirectories().getFiles()) {
            if (dir.isDirectory()) {
                classesPaths.add(dir.toPath());
            }
        }
        if (classesPaths.isEmpty()) {
            getLogger().lifecycle("coveragex: no production classes directories exist yet — skipping enrich.");
            return;
        }

        Path mapPath = getMapFile().isPresent() ? getMapFile().getAsFile().get().toPath() : null;

        EnrichmentPipelineRunner.Config config = new EnrichmentPipelineRunner.Config(
                dataFile,
                classesPaths,
                mapPath,
                getIncludes().get(),
                getExcludes().get());

        long started = System.nanoTime();
        EnrichmentPipelineRunner.Result result = new EnrichmentPipelineRunner().run(config);
        long elapsedMs = (System.nanoTime() - started) / 1_000_000;

        getLogger().lifecycle("coveragex: enrichment complete — included {} production classes, added {} zero-coverage classes in {} ms.",
                result.includedClassCount(), result.addedClassCount(), elapsedMs);
    }
}

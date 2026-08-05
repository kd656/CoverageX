package io.github.kd656.coveragex.gradle;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

/**
 * Configuration DSL for the CoverageX Gradle plugin. Configure via {@code coveragex { }}
 * in a build script. Property defaults are applied by {@link CoverageXPlugin} on apply.
 */
public abstract class CoverageXExtension {

    public abstract Property<Boolean> getSkip();

    public abstract RegularFileProperty getDestFile();

    public abstract RegularFileProperty getMapFile();

    /**
     * Combined semantic map (this module unioned with the maps of its upstream project
     * dependencies) that the agent reads at instrumentation time. Written by
     * {@code coveragexAnalyze}; not usually set by users.
     */
    public abstract RegularFileProperty getCombinedMapFile();

    public abstract DirectoryProperty getReportOutputDir();

    public abstract DirectoryProperty getSourceDirectory();

    public abstract ConfigurableFileCollection getClassesDirectories();

    public abstract ListProperty<String> getIncludes();

    public abstract ListProperty<String> getExcludes();

    public abstract ListProperty<String> getReportFormats();

    public abstract Property<Double> getMinimumCoverage();

    public abstract Property<Boolean> getFailOnLowCoverage();

    public abstract Property<Boolean> getEnableInvocationTracking();

    public abstract Property<Boolean> getEnableInsights();

    public abstract Property<Boolean> getEnableSuggestions();

    public abstract Property<Boolean> getEnableMCDC();

    public abstract Property<Boolean> getEnableOverCoverageAnalysis();

    /** {@code GLOBAL} or {@code PER_MODULE}. Applies only to aggregate reports. */
    public abstract Property<String> getThresholdMode();

    public abstract ListProperty<String> getExcludeSubprojects();

    /** Master switch: when false, the plugin attaches the agent to no {@code Test} task. */
    public abstract Property<Boolean> getConfigureTestTasks();

    /**
     * Narrower filter: when non-empty, the plugin only attaches the agent to {@code Test}
     * tasks whose names are in this list. Ignored when {@link #getConfigureTestTasks()} is false.
     */
    public abstract ListProperty<String> getTestTaskNames();

    /** When true, {@code coveragexReport} is wired as a dependency of Gradle's {@code check}. */
    public abstract Property<Boolean> getWireIntoCheck();
}

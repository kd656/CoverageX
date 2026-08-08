package io.github.kd656.coveragex.gradle.internal;

import io.github.kd656.coveragex.gradle.CoverageXExtension;
import io.github.kd656.coveragex.gradle.task.CoverageXAggregateReportTask;
import io.github.kd656.coveragex.gradle.task.CoverageXAnalyzeTask;
import io.github.kd656.coveragex.gradle.task.CoverageXEnrichTask;
import io.github.kd656.coveragex.gradle.task.CoverageXReportTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.testing.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static io.github.kd656.coveragex.gradle.internal.GradleModuleCoverageDiscoverer.COVERAGEX_PLUGIN_ID;

/**
 * Registers all CoverageX tasks on a project and wires them into the Gradle task graph.
 * Called by {@link io.github.kd656.coveragex.gradle.CoverageXPlugin} once the {@code java}
 * plugin is applied.
 */
public final class CoverageXTaskRegistrar {

    static final String ANALYZE_TASK = "coveragexAnalyze";
    static final String ENRICH_TASK = "coveragexEnrich";
    static final String REPORT_TASK = "coveragexReport";
    static final String AGGREGATE_REPORT_TASK = "coveragexAggregateReport";
    static final String VERIFY_TASK = "coveragexVerify";

    private final Project project;
    private final CoverageXExtension extension;
    private final ConfigurableFileCollection agentClasspath;

    public CoverageXTaskRegistrar(Project project,
                                  CoverageXExtension extension,
                                  ConfigurableFileCollection agentClasspath) {
        this.project = project;
        this.extension = extension;
        this.agentClasspath = agentClasspath;
    }

    public void register() {
        TaskProvider<CoverageXAnalyzeTask> analyze = registerAnalyze();
        TaskProvider<CoverageXEnrichTask> enrich = registerEnrich();
        TaskProvider<CoverageXReportTask> report = registerReport(enrich);
        registerVerify(report);

        wireUpstreamMaps(analyze);
        configureTestTasks(analyze, enrich);
        wireCheck(report);

        if (project == project.getRootProject()) {
            registerAggregateReport();
        }
    }

    private void registerAggregateReport() {
        TaskProvider<CoverageXAggregateReportTask> aggregate = project.getTasks().register(
                AGGREGATE_REPORT_TASK, CoverageXAggregateReportTask.class, task -> {
            task.setGroup("verification");
            task.setDescription("Aggregates CoverageX reports across every subproject that applies the plugin.");
            task.getReportOutputDir().set(extension.getReportOutputDir());
            task.getReportFormats().set(extension.getReportFormats());
            task.getIncludes().set(extension.getIncludes());
            task.getExcludes().set(extension.getExcludes());
            task.getExcludeSubprojects().set(extension.getExcludeSubprojects());
            task.getMinimumCoverage().set(extension.getMinimumCoverage());
            task.getFailOnLowCoverage().set(extension.getFailOnLowCoverage());
            task.getThresholdMode().set(extension.getThresholdMode());
            task.getEnableInvocationTracking().set(extension.getEnableInvocationTracking());
            task.getEnableInsights().set(extension.getEnableInsights());
            task.getEnableSuggestions().set(extension.getEnableSuggestions());
            task.getEnableMCDC().set(extension.getEnableMCDC());
            task.getEnableOverCoverageAnalysis().set(extension.getEnableOverCoverageAnalysis());
            task.getSkip().set(extension.getSkip());
            // Snapshot subproject descriptors at configuration time so the config cache can store them.
            task.getModuleDescriptors().set(project.provider(() ->
                    new GradleModuleCoverageDiscoverer(project.getRootProject()).discover()));
        });

        // Depend on each subproject's report as it applies the plugin. Concrete task
        // references keep this config-cache-safe (no captured root Project).
        project.getRootProject().getSubprojects().forEach(sp -> sp.getPlugins().withId(COVERAGEX_PLUGIN_ID,
                plugin -> aggregate.configure(t -> t.dependsOn(sp.getTasks().named(REPORT_TASK)))));
    }

    private TaskProvider<CoverageXAnalyzeTask> registerAnalyze() {
        return project.getTasks().register(ANALYZE_TASK, CoverageXAnalyzeTask.class, task -> {
            task.setGroup("verification");
            task.setDescription("Generates the CoverageX source-aware map.");
            task.getSourceDirectory().set(extension.getSourceDirectory());
            task.getMapFile().set(extension.getMapFile());
            task.getCombinedMapFile().set(extension.getCombinedMapFile());
            task.getSkip().set(extension.getSkip());
        });
    }

    /**
     * Feeds the maps of this project's CoverageX-enabled project dependencies into its own
     * {@code coveragexAnalyze}, so the agent labels upstream classes with their real source
     * text instead of an opcode fallback. Runs in {@code afterEvaluate} because dependencies
     * aren't declared yet when the plugin applies.
     */
    private void wireUpstreamMaps(TaskProvider<CoverageXAnalyzeTask> analyze) {
        project.afterEvaluate(evaluated -> {
            for (Project dependency : coverageXProjectDependencies(evaluated)) {
                dependency.getPlugins().withId(COVERAGEX_PLUGIN_ID, applied -> {
                    TaskProvider<CoverageXAnalyzeTask> upstreamAnalyze =
                            dependency.getTasks().named(ANALYZE_TASK, CoverageXAnalyzeTask.class);
                    analyze.configure(task -> {
                        task.getUpstreamMapFiles().from(
                                upstreamAnalyze.flatMap(CoverageXAnalyzeTask::getCombinedMapFile));
                        task.dependsOn(upstreamAnalyze);
                    });
                });
            }
        });
    }

    /** Distinct subprojects this project declares a {@code project(...)} dependency on. */
    private static Set<Project> coverageXProjectDependencies(Project project) {
        Set<Project> dependencies = new LinkedHashSet<>();
        for (String configurationName : List.of("api", "implementation", "compileOnly", "runtimeOnly")) {
            Configuration configuration = project.getConfigurations().findByName(configurationName);
            if (configuration == null) {
                continue;
            }
            for (Dependency dependency : configuration.getDependencies()) {
                if (dependency instanceof ProjectDependency projectDependency) {
                    Project dependencyProject = project.getRootProject()
                            .findProject(projectDependency.getPath());
                    if (dependencyProject != null && dependencyProject != project) {
                        dependencies.add(dependencyProject);
                    }
                }
            }
        }
        return dependencies;
    }

    private TaskProvider<CoverageXEnrichTask> registerEnrich() {
        return project.getTasks().register(ENRICH_TASK, CoverageXEnrichTask.class, task -> {
            task.setGroup("verification");
            task.setDescription("Adds zero-coverage production classes to CoverageX execution data.");
            task.getDestFile().set(extension.getDestFile());
            task.getClassesDirectories().from(extension.getClassesDirectories());
            task.getMapFile().set(extension.getMapFile());
            task.getIncludes().set(extension.getIncludes());
            task.getExcludes().set(extension.getExcludes());
            task.getSkip().set(extension.getSkip());
            // Depend on every Test task so `coveragexReport` (via enrich) runs the tests first.
            task.dependsOn(project.getTasks().withType(Test.class));
        });
    }

    private TaskProvider<CoverageXReportTask> registerReport(TaskProvider<CoverageXEnrichTask> enrich) {
        return project.getTasks().register(REPORT_TASK, CoverageXReportTask.class, task -> {
            task.setGroup("verification");
            task.setDescription("Generates the CoverageX report and enforces coverage thresholds.");
            task.dependsOn(enrich);
            task.getDestFile().set(extension.getDestFile());
            task.getMapFile().set(extension.getMapFile());
            task.getClassesDirectories().from(extension.getClassesDirectories());
            task.getReportOutputDir().set(extension.getReportOutputDir());
            task.getSourceDirectory().set(extension.getSourceDirectory());
            task.getReportFormats().set(extension.getReportFormats());
            task.getIncludes().set(extension.getIncludes());
            task.getExcludes().set(extension.getExcludes());
            task.getMinimumCoverage().set(extension.getMinimumCoverage());
            task.getFailOnLowCoverage().set(extension.getFailOnLowCoverage());
            task.getThresholdMode().set(extension.getThresholdMode());
            task.getEnableInvocationTracking().set(extension.getEnableInvocationTracking());
            task.getEnableInsights().set(extension.getEnableInsights());
            task.getEnableSuggestions().set(extension.getEnableSuggestions());
            task.getEnableMCDC().set(extension.getEnableMCDC());
            task.getEnableOverCoverageAnalysis().set(extension.getEnableOverCoverageAnalysis());
            task.getSkip().set(extension.getSkip());
            task.getProjectName().set(project.getName());
            task.getProjectBaseDir().set(project.getLayout().getProjectDirectory());
        });
    }

    private void registerVerify(TaskProvider<CoverageXReportTask> report) {
        project.getTasks().register(VERIFY_TASK, task -> {
            task.setGroup("verification");
            task.setDescription("Runs the full CoverageX lifecycle: analyze, tests, enrich, report.");
            task.dependsOn(report);
        });
    }

    private void configureTestTasks(TaskProvider<CoverageXAnalyzeTask> analyze,
                                    TaskProvider<CoverageXEnrichTask> enrich) {
        project.getTasks().withType(Test.class).configureEach(test -> {
            if (!shouldAttachAgentTo(test.getName())) {
                return;
            }

            test.dependsOn(analyze);

            CoverageXAgentArgumentProvider argumentProvider = project.getObjects()
                    .newInstance(CoverageXAgentArgumentProvider.class);
            argumentProvider.getAgentClasspath().from(agentClasspath);
            argumentProvider.getDestFile().set(extension.getDestFile());
            // Agent reads the combined map so dependency classes keep their real condition text.
            argumentProvider.getMapFile().set(extension.getCombinedMapFile());
            argumentProvider.getIncludes().set(extension.getIncludes());
            argumentProvider.getExcludes().set(extension.getExcludes());
            argumentProvider.getSkip().set(extension.getSkip());

            test.getJvmArgumentProviders().add(argumentProvider);
        });
    }

    private boolean shouldAttachAgentTo(String testTaskName) {
        if (!extension.getConfigureTestTasks().get()) {
            return false;
        }
        List<String> allowlist = extension.getTestTaskNames().get();
        return allowlist.isEmpty() || allowlist.contains(testTaskName);
    }

    private void wireCheck(TaskProvider<CoverageXReportTask> report) {
        project.getTasks().named("check").configure(task -> task.dependsOn(project.provider(() ->
                extension.getWireIntoCheck().get() ? List.of(report) : List.of())));
    }
}

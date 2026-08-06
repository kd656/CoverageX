package io.github.kd656.coveragex.gradle;

import io.github.kd656.coveragex.gradle.internal.CoverageXTaskRegistrar;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.plugins.JavaPluginExtension;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

/**
 * Gradle plugin entry point for CoverageX. Registers the {@code coveragex} extension and the
 * {@code coveragexAgent} configuration, then wires the tasks once the {@code java} plugin applies.
 */
public final class CoverageXPlugin implements Plugin<Project> {

    /** Agent version, stamped into {@code coveragex-build.properties} by the build — no hardcoded copy. */
    private static final String COVERAGEX_VERSION = loadVersion();

    static final String EXTENSION_NAME = "coveragex";
    static final String AGENT_CONFIGURATION_NAME = "coveragexAgent";

    @Override
    public void apply(Project project) {
        CoverageXExtension extension = createExtension(project);
        Configuration agentConfiguration = createAgentConfiguration(project);

        project.getPlugins().withId("java", plugin -> {
            JavaPluginExtension java = project.getExtensions().getByType(JavaPluginExtension.class);
            extension.getClassesDirectories()
                    .from(java.getSourceSets().getByName("main").getOutput().getClassesDirs());

            ConfigurableFileCollection agentClasspath = project.files(agentConfiguration);
            new CoverageXTaskRegistrar(project, extension, agentClasspath).register();
        });
    }

    private CoverageXExtension createExtension(Project project) {
        CoverageXExtension extension = project.getExtensions()
                .create(EXTENSION_NAME, CoverageXExtension.class);

        extension.getSkip().convention(false);
        extension.getDestFile().convention(
                project.getLayout().getBuildDirectory().file("coveragex/coveragex.exec"));
        extension.getMapFile().convention(
                project.getLayout().getBuildDirectory().file("classes/java/test/coveragex/coveragex.map.json"));
        extension.getCombinedMapFile().convention(
                project.getLayout().getBuildDirectory().file("coveragex/coveragex.map.combined.json"));
        extension.getReportOutputDir().convention(
                project.getLayout().getBuildDirectory().dir("reports/coveragex"));
        extension.getSourceDirectory().convention(
                project.getLayout().getProjectDirectory().dir("src/main/java"));
        extension.getIncludes().convention(List.of());
        extension.getExcludes().convention(List.of());
        extension.getReportFormats().convention(List.of("html"));
        extension.getMinimumCoverage().convention(0.0);
        extension.getFailOnLowCoverage().convention(false);
        extension.getEnableInvocationTracking().convention(false);
        extension.getEnableInsights().convention(false);
        extension.getEnableSuggestions().convention(false);
        extension.getEnableMCDC().convention(false);
        extension.getEnableOverCoverageAnalysis().convention(false);
        extension.getThresholdMode().convention("GLOBAL");
        extension.getExcludeSubprojects().convention(List.of());
        extension.getConfigureTestTasks().convention(true);
        extension.getTestTaskNames().convention(List.of());
        extension.getWireIntoCheck().convention(true);

        return extension;
    }

    private Configuration createAgentConfiguration(Project project) {
        Configuration configuration = project.getConfigurations().create(AGENT_CONFIGURATION_NAME);
        configuration.setCanBeConsumed(false);
        configuration.setCanBeResolved(true);
        configuration.setDescription("CoverageX agent jar attached to Test task JVMs.");
        configuration.defaultDependencies(dependencies -> dependencies.add(
                project.getDependencies().create("io.github.kd656:coveragex-agent:" + COVERAGEX_VERSION)));
        return configuration;
    }

    private static String loadVersion() {
        try (InputStream in = CoverageXPlugin.class.getResourceAsStream("/coveragex-build.properties")) {
            if (in == null) {
                throw new IllegalStateException("coveragex-build.properties not found on the plugin classpath");
            }
            Properties properties = new Properties();
            properties.load(in);
            String version = properties.getProperty("version");
            if (version == null || version.isBlank()) {
                throw new IllegalStateException("coveragex-build.properties is missing the 'version' entry");
            }
            return version;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read coveragex-build.properties", e);
        }
    }
}

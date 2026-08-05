package io.github.kd656.coveragex.gradle;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CoverageXPluginTest {

    private Project project;

    @BeforeEach
    void setUp() {
        project = ProjectBuilder.builder().build();
        project.getPlugins().apply("java");
        project.getPlugins().apply("io.github.kd656.coveragex");
    }

    @Test
    void registersExtensionUnderCoveragexName() {
        assertThat(project.getExtensions().findByName("coveragex")).isNotNull();
    }

    @Test
    void extensionDefaultsMatchPlan() {
        CoverageXExtension ext = project.getExtensions().getByType(CoverageXExtension.class);

        assertThat(ext.getSkip().get()).isFalse();
        assertThat(ext.getIncludes().get()).isEmpty();
        assertThat(ext.getExcludes().get()).isEmpty();
        assertThat(ext.getReportFormats().get()).containsExactly("html");
        assertThat(ext.getMinimumCoverage().get()).isEqualTo(0.0);
        assertThat(ext.getFailOnLowCoverage().get()).isFalse();
        assertThat(ext.getThresholdMode().get()).isEqualTo("GLOBAL");
        assertThat(ext.getConfigureTestTasks().get()).isTrue();
        assertThat(ext.getTestTaskNames().get()).isEmpty();
        assertThat(ext.getWireIntoCheck().get()).isTrue();
    }

    @Test
    void destFileDefaultsToBuildCoveragexExec() {
        CoverageXExtension ext = project.getExtensions().getByType(CoverageXExtension.class);
        assertThat(ext.getDestFile().get().getAsFile().getPath())
                .endsWith("build/coveragex/coveragex.exec".replace('/', java.io.File.separatorChar));
    }

    @Test
    void createsResolvableCoveragexAgentConfiguration() {
        var configuration = project.getConfigurations().findByName("coveragexAgent");
        assertThat(configuration).isNotNull();
        assertThat(configuration.isCanBeResolved()).isTrue();
        assertThat(configuration.isCanBeConsumed()).isFalse();
    }

    @Test
    void wiresJavaMainClassesIntoExtension() {
        CoverageXExtension ext = project.getExtensions().getByType(CoverageXExtension.class);
        assertThat(ext.getClassesDirectories().getFiles()).isNotEmpty();
    }
}

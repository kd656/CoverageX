package io.github.kd656.coveragex.gradle;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CoverageXTaskRegistrationTest {

    private Project project;

    @BeforeEach
    void setUp() {
        project = ProjectBuilder.builder().build();
        project.getPlugins().apply("java");
        project.getPlugins().apply("io.github.kd656.coveragex");
    }

    @Test
    void registersAllExpectedTasks() {
        assertThat(project.getTasks().findByName("coveragexAnalyze")).isNotNull();
        assertThat(project.getTasks().findByName("coveragexEnrich")).isNotNull();
        assertThat(project.getTasks().findByName("coveragexReport")).isNotNull();
        assertThat(project.getTasks().findByName("coveragexVerify")).isNotNull();
    }

    @Test
    void reportDependsOnEnrich() {
        Task report = project.getTasks().getByName("coveragexReport");
        assertThat(taskDependencyNames(report)).contains("coveragexEnrich");
    }

    @Test
    void verifyDependsOnReport() {
        Task verify = project.getTasks().getByName("coveragexVerify");
        assertThat(taskDependencyNames(verify)).contains("coveragexReport");
    }

    @Test
    void checkDependsOnReportByDefault() {
        Task check = project.getTasks().getByName("check");
        assertThat(taskDependencyNames(check)).contains("coveragexReport");
    }

    @Test
    void allTasksLandInVerificationGroup() {
        assertThat(project.getTasks().getByName("coveragexAnalyze").getGroup()).isEqualTo("verification");
        assertThat(project.getTasks().getByName("coveragexEnrich").getGroup()).isEqualTo("verification");
        assertThat(project.getTasks().getByName("coveragexReport").getGroup()).isEqualTo("verification");
        assertThat(project.getTasks().getByName("coveragexVerify").getGroup()).isEqualTo("verification");
    }

    private static java.util.Set<String> taskDependencyNames(Task task) {
        java.util.Set<String> names = new java.util.HashSet<>();
        for (Object dep : task.getTaskDependencies().getDependencies(task)) {
            if (dep instanceof Task t) {
                names.add(t.getName());
            }
        }
        return names;
    }
}

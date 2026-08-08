package io.github.kd656.coveragex.gradle;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Multi-project smoke: applies the plugin to root + two subprojects, then asserts the
 * root has {@code coveragexAggregateReport} and the subprojects only have
 * {@code coveragexReport} (no aggregate). Does not execute the full coverage flow — that
 * requires the coveragex-agent jar in the local Maven repository (covered by the example
 * project builds in CI, §9.2).
 */
class CoverageXAggregateSmokeTest {

    @TempDir Path projectDir;

    @Test
    void aggregateRegisteredOnRootAndDependsOnSubprojectReports() throws IOException {
        Files.writeString(projectDir.resolve("settings.gradle.kts"), """
                dependencyResolutionManagement {
                    repositories {
                        mavenLocal()
                        mavenCentral()
                    }
                }
                rootProject.name = "aggregate-smoke"
                include("dto", "service")
                """);
        Files.writeString(projectDir.resolve("build.gradle.kts"), """
                plugins {
                    java
                    id("io.github.kd656.coveragex")
                }
                subprojects {
                    apply(plugin = "java")
                    apply(plugin = "io.github.kd656.coveragex")
                }
                """);
        Files.createDirectories(projectDir.resolve("dto"));
        Files.createDirectories(projectDir.resolve("service"));

        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withArguments(":tasks", "--group=verification", "--all", "--configuration-cache", "--stacktrace")
                .withPluginClasspath()
                .forwardOutput()
                .build();

        assertThat(result.getOutput()).contains("coveragexAggregateReport");

        BuildResult dryRun = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withArguments(":coveragexAggregateReport", "--dry-run", "--configuration-cache", "--stacktrace")
                .withPluginClasspath()
                .forwardOutput()
                .build();

        String out = dryRun.getOutput();
        assertThat(out).contains(":dto:coveragexReport");
        assertThat(out).contains(":service:coveragexReport");
        assertThat(out).contains(":coveragexAggregateReport");
    }

    @Test
    void aggregateNotRegisteredOnSubprojects() throws IOException {
        Files.writeString(projectDir.resolve("settings.gradle.kts"), """
                dependencyResolutionManagement {
                    repositories {
                        mavenLocal()
                        mavenCentral()
                    }
                }
                rootProject.name = "sub-only"
                include("child")
                """);
        Files.writeString(projectDir.resolve("build.gradle.kts"), """
                plugins {
                    java
                    id("io.github.kd656.coveragex") apply false
                }
                subprojects {
                    apply(plugin = "java")
                    apply(plugin = "io.github.kd656.coveragex")
                }
                """);
        Files.createDirectories(projectDir.resolve("child"));

        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withArguments(":child:tasks", "--group=verification", "--all", "--stacktrace")
                .withPluginClasspath()
                .forwardOutput()
                .build();

        assertThat(result.getOutput()).doesNotContain("coveragexAggregateReport");
        assertThat(result.getOutput()).contains("coveragexReport");
    }
}

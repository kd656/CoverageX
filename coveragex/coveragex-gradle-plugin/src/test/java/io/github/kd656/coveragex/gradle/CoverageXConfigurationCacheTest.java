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
 * Real end-to-end configuration cache verification: runs a task action twice and asserts
 * the cache is stored on the first run and reused on the second. Static audits can only
 * find obvious violations; this catches captured state that fails at serialization time.
 */
class CoverageXConfigurationCacheTest {

    @TempDir Path projectDir;

    @Test
    void coveragexReportIsConfigurationCacheCompatible() throws IOException {
        Files.writeString(projectDir.resolve("settings.gradle.kts"), """
                dependencyResolutionManagement {
                    repositories {
                        mavenLocal()
                        mavenCentral()
                    }
                }
                rootProject.name = "cc-single"
                """);
        Files.writeString(projectDir.resolve("build.gradle.kts"), """
                plugins {
                    java
                    id("io.github.kd656.coveragex")
                }
                """);

        BuildResult first = runReport();
        assertThat(first.getOutput()).contains("Configuration cache entry stored");

        BuildResult second = runReport();
        assertThat(second.getOutput()).contains("Configuration cache entry reused");
    }

    @Test
    void coveragexAggregateReportIsConfigurationCacheCompatible() throws IOException {
        Files.writeString(projectDir.resolve("settings.gradle.kts"), """
                dependencyResolutionManagement {
                    repositories {
                        mavenLocal()
                        mavenCentral()
                    }
                }
                rootProject.name = "cc-aggregate"
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

        // The aggregate task will fail loudly if no exec files exist, but the configuration
        // cache is stored during task-graph preparation — before execution. Using --dry-run
        // exercises the store path without triggering the "no exec files found" GradleException.
        BuildResult first = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withArguments(":coveragexAggregateReport", "--dry-run", "--configuration-cache", "--stacktrace")
                .withPluginClasspath()
                .forwardOutput()
                .build();
        assertThat(first.getOutput()).contains("Configuration cache entry stored");

        BuildResult second = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withArguments(":coveragexAggregateReport", "--dry-run", "--configuration-cache", "--stacktrace")
                .withPluginClasspath()
                .forwardOutput()
                .build();
        assertThat(second.getOutput()).contains("Configuration cache entry reused");
    }

    private BuildResult runReport() {
        return GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withArguments(":coveragexReport", "--configuration-cache", "--stacktrace")
                .withPluginClasspath()
                .forwardOutput()
                .build();
    }
}

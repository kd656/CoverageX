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
 * Smoke test: spawn a real Gradle daemon in a temp dir with a build script that applies
 * the plugin, then confirm all CoverageX tasks are registered and picked up by
 * {@code ./gradlew tasks}. Does not exercise the end-to-end coverage flow — that requires
 * the coveragex-agent jar in the local Maven repository and is covered by the example
 * project builds in CI (§9.2).
 */
class CoverageXTestKitSmokeTest {

    @TempDir Path projectDir;

    @Test
    void pluginAppliesAndRegistersTasks() throws IOException {
        Files.writeString(projectDir.resolve("settings.gradle.kts"), """
                dependencyResolutionManagement {
                    repositories {
                        mavenLocal()
                        mavenCentral()
                    }
                }
                rootProject.name = "smoke"
                """);
        Files.writeString(projectDir.resolve("build.gradle.kts"), """
                plugins {
                    java
                    id("io.github.kd656.coveragex")
                }
                """);

        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withArguments("tasks", "--group=verification", "--configuration-cache", "--stacktrace")
                .withPluginClasspath()
                .forwardOutput()
                .build();

        String output = result.getOutput();
        assertThat(output).contains("coveragexAnalyze");
        assertThat(output).contains("coveragexEnrich");
        assertThat(output).contains("coveragexReport");
        assertThat(output).contains("coveragexVerify");
    }
}

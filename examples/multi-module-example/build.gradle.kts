plugins {
    java
    id("io.github.kd656.coveragex") version "0.2.0-SNAPSHOT"
}

allprojects {
    group = "example.coveragex.multimodule"
    version = "0.2.0-SNAPSHOT"
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.github.kd656.coveragex")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        // Gradle 9 fails empty test discovery; the dto subproject is DTO-only.
        failOnNoDiscoveredTests = false
    }

    extensions.configure<io.github.kd656.coveragex.gradle.CoverageXExtension> {
        includes.set(listOf("example.**"))
    }
}

// Root-only aggregate configuration.
coveragex {
    thresholdMode.set("PER_MODULE")
    reportFormats.set(listOf("html"))
    enableInvocationTracking.set(true)
    enableInsights.set(true)
    enableSuggestions.set(true)
    enableOverCoverageAnalysis.set(true)
    minimumCoverage.set(0.0)
    failOnLowCoverage.set(false)
}

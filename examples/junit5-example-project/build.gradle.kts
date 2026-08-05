plugins {
    java
    id("io.github.kd656.coveragex") version "0.1.0-SNAPSHOT"
}

group = "org.example"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.11.0-M2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.11.0-M2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.0-M2")
    testImplementation("io.github.kd656:coveragex-test-junit5:0.1.0-SNAPSHOT")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

coveragex {
    reportFormats.set(listOf("html"))
    enableInvocationTracking.set(true)
    enableInsights.set(true)
    enableSuggestions.set(true)
    enableOverCoverageAnalysis.set(true)
    includes.set(listOf("org.example.**"))
    excludes.set(listOf("**.Test", "**.*Test", "**.Tests", "**.*Tests"))
}

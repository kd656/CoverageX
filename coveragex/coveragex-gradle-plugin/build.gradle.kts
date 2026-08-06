plugins {
    `java-gradle-plugin`
    `maven-publish`
}

group = "io.github.kd656"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

gradlePlugin {
    plugins.create("coveragex") {
        id = "io.github.kd656.coveragex"
        implementationClass = "io.github.kd656.coveragex.gradle.CoverageXPlugin"
        displayName = "CoverageX"
        description = "Java code coverage plugin backed by the CoverageX agent."
    }
}

dependencies {
    implementation("io.github.kd656:coveragex-api:${project.version}")
    implementation("io.github.kd656:coveragex-core:${project.version}")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation(gradleTestKit())
}

// Stamp the plugin's own version into coveragex-build.properties so the plugin can
// resolve the coveragex-agent coordinate at runtime without a hand-maintained constant.
tasks.named<ProcessResources>("processResources") {
    val pluginVersion = project.version.toString()
    inputs.property("pluginVersion", pluginVersion)
    filesMatching("coveragex-build.properties") {
        expand("version" to pluginVersion)
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

publishing {
    repositories {
        maven {
            name = "centralPortalSnapshots"
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
            credentials {
                username = System.getenv("CENTRAL_PORTAL_USERNAME")
                password = System.getenv("CENTRAL_PORTAL_PASSWORD")
            }
        }
    }
}

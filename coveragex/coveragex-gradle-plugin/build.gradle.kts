import com.vanniktech.maven.publish.SonatypeHost

plugins {
    `java-gradle-plugin`
    id("com.vanniktech.maven.publish") version "0.30.0"
}

group = "io.github.kd656"
version = "0.2.0-SNAPSHOT"

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

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = false)

    // Only sign when a key is supplied
    if (project.hasProperty("signingInMemoryKey")) {
        signAllPublications()
    }

    coordinates(group.toString(), "coveragex-gradle-plugin", version.toString())

    pom {
        name.set("CoverageX Gradle Plugin")
        description.set("Java code coverage plugin backed by the CoverageX agent.")
        url.set("https://github.com/kd656/CoverageX")

        licenses {
            license {
                name.set("FSL-1.1-ALv2")
                url.set("https://github.com/kd656/CoverageX/blob/main/LICENSE.md")
                distribution.set("repo")
            }
        }

        developers {
            developer {
                id.set("kd656")
                name.set("kd656")
                url.set("https://github.com/kd656")
            }
        }

        scm {
            url.set("https://github.com/kd656/CoverageX")
            connection.set("scm:git:https://github.com/kd656/CoverageX.git")
            developerConnection.set("scm:git:git@github.com:kd656/CoverageX.git")
        }
    }
}

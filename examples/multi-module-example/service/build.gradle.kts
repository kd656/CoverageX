dependencies {
    implementation(project(":dto"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.0")
    // Registers the CoverageX JUnit 5 test listener. Without it the agent has
    // no way to attribute a probe hit to a specific test method.
    testImplementation("io.github.kd656:coveragex-test-junit5:0.1.0-SNAPSHOT")
}

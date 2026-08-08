package io.github.kd656.coveragex.gradle.internal;

import org.gradle.api.file.FileCollection;

import java.io.File;

/**
 * Resolves the CoverageX agent shaded JAR from a resolved {@code coveragexAgent} configuration.
 */
public final class AgentJarResolver {

    private AgentJarResolver() {}

    public static File resolve(FileCollection agentClasspath) {
        for (File file : agentClasspath.getFiles()) {
            String name = file.getName();
            if (name.startsWith("coveragex-agent") && name.endsWith(".jar")) {
                return file;
            }
        }
        throw new IllegalStateException(
                "coveragex-agent jar not found on the coveragexAgent configuration. "
                        + "Files: " + agentClasspath.getFiles());
    }
}

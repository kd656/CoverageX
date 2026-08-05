package io.github.kd656.coveragex.gradle.internal;

import io.github.kd656.coveragex.api.agent.AgentArgumentBuilder;
import io.github.kd656.coveragex.api.agent.AgentOptions;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Internal;
import org.gradle.process.CommandLineArgumentProvider;

import java.util.List;

/**
 * Supplies the {@code -javaagent} argument for {@code Test} tasks.
 */
public abstract class CoverageXAgentArgumentProvider implements CommandLineArgumentProvider {

    @Classpath
    public abstract ConfigurableFileCollection getAgentClasspath();

    /** Coverage output path. {@code @Internal}: the file doesn't exist until the test runs. */
    @Internal
    public abstract RegularFileProperty getDestFile();

    /** Map path passed to the agent; also absent when the test starts. */
    @Internal
    public abstract RegularFileProperty getMapFile();

    @Internal
    public abstract ListProperty<String> getIncludes();

    @Internal
    public abstract ListProperty<String> getExcludes();

    @Internal
    public abstract Property<Boolean> getSkip();

    @Override
    public Iterable<String> asArguments() {
        if (getSkip().get()) {
            return List.of();
        }

        String agentJar = AgentJarResolver.resolve(getAgentClasspath()).getAbsolutePath();
        String destFile = getDestFile().getAsFile().get().getAbsolutePath();
        String mapFile = getMapFile().getAsFile().get().getAbsolutePath();

        String options = new AgentArgumentBuilder().build(
                new AgentOptions(destFile, mapFile, getIncludes().get(), getExcludes().get()));

        return List.of("-javaagent:" + agentJar + "=" + options);
    }
}

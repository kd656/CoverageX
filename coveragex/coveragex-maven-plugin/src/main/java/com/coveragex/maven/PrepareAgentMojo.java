package com.coveragex.maven;

import com.coveragex.api.agent.AgentArgumentBuilder;
import com.coveragex.api.agent.AgentOptions;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

/**
 * Prepares the CoverageX Java agent for test execution.
 *
 * <p>This goal sets up the {@code argLine} property to launch the Surefire JVM with the
 * CoverageX agent attached. The agent instruments classes at runtime during test execution.</p>
 */
@Mojo(name = "prepare-agent", defaultPhase = LifecyclePhase.INITIALIZE, threadSafe = true)
public class PrepareAgentMojo extends AbstractMojo {

    /**
     * The Maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * The plugin descriptor (for accessing plugin dependencies).
     */
    @Parameter(defaultValue = "${plugin}", readonly = true, required = true)
    private PluginDescriptor pluginDescriptor;

    /**
     * Name of the coverage data file.
     */
    @Parameter(property = "coveragex.destFile", defaultValue = "coveragex.exec")
    private String destFile;

    /**
     * The property name to append the agent configuration to.
     * Surefire automatically reads this property.
     */
    @Parameter(property = "coveragex.propertyName", defaultValue = "argLine")
    private String propertyName;

    /**
     * Skip execution of this goal.
     */
    @Parameter(property = "coveragex.skip", defaultValue = "false")
    private boolean skip;

    /**
     * List of class patterns to include in coverage analysis.
     * Supports wildcards: * (single package level) and ** (any depth).
     * Example: org.example.**, com.myapp.*
     */
    @Parameter(property = "coveragex.includes")
    private List<String> includes;

    /**
     * List of class patterns to exclude from coverage analysis.
     * Excludes take precedence over includes.
     * Example: **.Test, **.Tests, **.*Test
     */
    @Parameter(property = "coveragex.excludes")
    private List<String> excludes;

    @Override
    public void execute() throws MojoExecutionException {
        Log log = getLog();
        if (skip) {
            log.info("Skipping CoverageX agent preparation");
            return;
        }

        // Resolve agent JAR from plugin classpath
        String agentJarPath = resolveAgentJar();
        if (agentJarPath == null) {
            throw new MojoExecutionException(
                    "Could not find coveragex-agent.jar in plugin classpath. " +
                    "Ensure coveragex-agent is listed as a plugin dependency.");
        }

        // Build agent arguments
        String destFilePath = Paths.get(project.getBuild().getDirectory(), destFile).toAbsolutePath().toString();

        // The coverage map is written by the 'analyze' goal to the test-classes directory.
        // Pass it to the agent so source-aware probe injection can be activated.
        String mapFilePath = Paths.get(project.getBuild().getDirectory(), "test-classes", "coveragex", "coveragex.map.json").toAbsolutePath().toString();

        String agentArgs = new AgentArgumentBuilder().build(
                new AgentOptions(destFilePath, mapFilePath, includes, excludes));

        // Build complete agent argument string. Quote it as one JVM argument so paths
        // containing spaces do not get split by Surefire's argLine parser.
        String agentArgument = quoteForSurefireArgLine(String.format("-javaagent:%s=%s", agentJarPath, agentArgs));

        // Append to existing argLine (for JaCoCo compatibility)
        String existingArgLine = project.getProperties().getProperty(propertyName, "").trim();
        String newArgLine = existingArgLine.isEmpty()
                ? agentArgument
                : existingArgLine + " " + agentArgument;

        project.getProperties().setProperty(propertyName, newArgLine);

        log.info("CoverageX agent prepared: " + agentArgument);
        if (includes != null && !includes.isEmpty()) {
            log.info("  Includes: " + includes);
        }
        if (excludes != null && !excludes.isEmpty()) {
            log.info("  Excludes: " + excludes);
        }
        log.debug("Updated " + propertyName + " = " + newArgLine);
    }

    private String quoteForSurefireArgLine(String argument) {
        return "\"" + argument.replace("\"", "\\\"") + "\"";
    }

    /**
     * Resolves the path to the CoverageX agent JAR from the plugin classpath.
     *
     * @return the absolute path to the agent JAR, or null if not found
     */
    private String resolveAgentJar() {
        // First, try to find it in the plugin's own dependencies
        if (pluginDescriptor != null) {
            List<Artifact> pluginArtifacts = pluginDescriptor.getArtifacts();
            if (pluginArtifacts != null) {
                for (Artifact artifact : pluginArtifacts) {
                    if ("coveragex-agent".equals(artifact.getArtifactId()) &&
                        "com.coveragex".equals(artifact.getGroupId())) {
                        File file = artifact.getFile();
                        if (file != null && file.exists()) {
                            String path = file.getAbsolutePath();
                            getLog().debug("Found agent JAR from plugin dependencies: " + path);
                            return path;
                        }
                    }
                }
            }
        }

        // Fallback: Search plugin classpath for coveragex-agent.jar
        String classPath = System.getProperty("java.class.path");
        if (classPath != null) {
            for (String path : classPath.split(File.pathSeparator)) {
                if (path.contains("coveragex-agent") && path.endsWith(".jar")) {
                    getLog().debug("Found agent JAR in classpath: " + path);
                    return path;
                }
            }
        }

        return null;
    }
}

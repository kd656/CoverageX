package io.github.kd656.coveragex.agent;

import io.github.kd656.coveragex.core.collect.CommonCoverageDataCollector;
import io.github.kd656.coveragex.core.collect.CoverageDataCollectorDelegate;
import io.github.kd656.coveragex.core.instrument.ClassTransformer;
import io.github.kd656.coveragex.api.agent.AgentOptions;
import io.github.kd656.coveragex.api.io.internal.BinaryDataWriter;
import io.github.kd656.coveragex.test.api.ProbeExecutionContextProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * JVM agent entry point for CoverageX runtime instrumentation.
 *
 * <p>Attach via {@code -javaagent:coveragex-agent.jar[=options]}.</p>
 */
public class CoverageAgent {

    private static final Logger LOG = LoggerFactory.getLogger(CoverageAgent.class);

    private static volatile boolean initialized = false;

    /**
     * JVM agent premain entry point.
     *
     * @param agentArgs       agent arguments from the command line
     * @param instrumentation the JVM instrumentation instance
     */
    public static void premain(String agentArgs, Instrumentation instrumentation) {
        if (initialized) {
            LOG.warn("CoverageX agent already initialized, skipping");
            return;
        }

        LOG.info("CoverageX agent starting with args: {}", agentArgs);

        AgentOptions options = new AgentArgumentParser().parse(agentArgs);

        if (options.destFile() != null && !options.destFile().isBlank()) {
            System.setProperty("coveragex.destFile", options.destFile());
            LOG.debug("Set destFile: {}", options.destFile());
        }

        List<String> includes = options.includes();
        List<String> excludes = options.excludes();

        String mapFileStr = options.mapFile();
        Path coverageMapPath = mapFileStr != null ? Paths.get(mapFileStr) : null;

        // TODO: Add configurations to choose implementations
        BinaryDataWriter binaryDataWriter = new BinaryDataWriter();
        var collector = new CommonCoverageDataCollector(
                binaryDataWriter,
                () -> CoverageDataCollectorDelegate.contextRegistry().current());
        CoverageDataCollectorDelegate.registry().installGlobal(collector);

        initializeTestTracking();

        var transformer = new ClassTransformer(collector, includes, excludes, coverageMapPath);

        instrumentation.addTransformer(transformer, true);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOG.info("CoverageX agent shutting down, flushing coverage data");
            collector.flush();
        }, "coveragex-shutdown"));

        initialized = true;
        LOG.info("CoverageX agent initialized successfully");
        if (!includes.isEmpty()) {
            LOG.info("  Includes: {}", includes);
        }
        if (!excludes.isEmpty()) {
            LOG.info("  Excludes: {}", excludes);
        }
        if (coverageMapPath != null) {
            LOG.info("  Coverage map: {}", coverageMapPath);
        } else {
            LOG.info("  Coverage map: not configured (source-aware instrumentation disabled)");
        }
    }

    private static void initializeTestTracking() {
        try {
            Class.forName("io.github.kd656.coveragex.test.api.TestContextHolder", false,
                    ClassLoader.getSystemClassLoader());

            ServiceLoader<ProbeExecutionContextProvider> loader;
            try {
                loader = ServiceLoader.load(ProbeExecutionContextProvider.class,
                        ClassLoader.getSystemClassLoader());
            } catch (Exception e) {
                LOG.warn("CoverageX: ServiceLoader.load(ProbeExecutionContextProvider) failed; "
                        + "test attribution will be empty.", e);
                return;
            }

            for (ProbeExecutionContextProvider probeExecutionContextProvider : loader) {
                String name = probeExecutionContextProvider.getClass().getName();

                try {
                    CoverageDataCollectorDelegate.contextRegistry()
                            .installGlobal(probeExecutionContextProvider::currentContext);
                    LOG.info("CoverageX: registered context provider: {}", name);
                    return;
                } catch (Exception e) {
                    LOG.warn("CoverageX: skipping provider: {}", name, e);
                }
            }

            LOG.warn("CoverageX: no ProbeExecutionContextProvider on classpath; "
                    + "test attribution will be empty. "
                    + "Add coveragex-test-junit5 (or another provider) to the test classpath to enable.");

        } catch (ClassNotFoundException e) {
            LOG.debug("CoverageX: coveragex-test-api is not on classpath - test tracking is disabled.");
        }
    }

    /**
     * Returns whether the agent has been initialized.
     */
    public static boolean isInitialized() {
        return initialized;
    }

}

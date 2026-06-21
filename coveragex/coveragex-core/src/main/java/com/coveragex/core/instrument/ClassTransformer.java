package com.coveragex.core.instrument;

import com.coveragex.core.collect.CommonCoverageDataCollector;
import com.coveragex.core.analysis.source.CoverageContextResolver;
import com.coveragex.core.analysis.source.model.ClassModel;
import com.coveragex.core.scan.ClassCoverageFilter;
import com.coveragex.core.scan.ClassOrigin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.ClassFileTransformer;
import java.nio.file.Path;
import java.security.ProtectionDomain;
import java.util.List;

/**
 * Transforms class bytecode to inject coverage probes during class loading.
 *
 * <p>The transformer applies a two-path strategy based on source map availability:</p>
 * <ul>
 *   <li>When a {@link ClassModel} is found in the coverage map for the class being
 *       loaded, {@link SourceAwareProbeInjector} is used and branch probes are
 *       populated with verbatim source condition text (e.g. {@code "name == null"}).</li>
 *   <li>When no class model is available (generated classes, classes outside the
 *       analyzed source roots, or when the map file is absent), {@link DefaultProbeInjector}
 *       is used as a fallback and branch probes receive generic opcode placeholders
 *       (e.g. {@code "if (x == null)"}).</li>
 * </ul>
 *
 * <p>Include/exclude glob patterns control which classes are instrumented at all.
 * Test output directories are detected via {@code ProtectionDomain} and excluded
 * unconditionally.</p>
 */
public class ClassTransformer implements ClassFileTransformer {

    private static final Logger LOG = LoggerFactory.getLogger(ClassTransformer.class);

    /** Fallback injector: used when no class model is available in the coverage map. */
    private final DefaultProbeInjector defaultProbeInjector;

    /** Source-aware injector: used when a ClassModel is found for the class being loaded. */
    private final SourceAwareProbeInjector sourceAwareProbeInjector;

    private final CoverageContextResolver coverageContextResolver;

    private final ClassCoverageFilter classCoverageFilter;

    /**
     * Constructs a {@code ClassTransformer} with the given collector, filter patterns,
     * and path to the optional coverage map file.
     *
     * @param collector       the probe-hit collector shared across all instrumented classes
     * @param includes        glob patterns for classes to include (empty = include all)
     * @param excludes        glob patterns for classes to exclude
     * @param coverageMapPath path to {@code coveragex.map.json}, or {@code null} when
     *                        source-aware instrumentation is not available
     */
    public ClassTransformer(CommonCoverageDataCollector collector, List<String> includes, List<String> excludes, Path coverageMapPath) {
        this.defaultProbeInjector = new DefaultProbeInjector(collector);
        this.sourceAwareProbeInjector = new SourceAwareProbeInjector(collector);
        this.coverageContextResolver = new CoverageContextResolver(coverageMapPath);

        this.classCoverageFilter = new ClassCoverageFilter(includes, excludes);
    }

    /**
     * Transforms the given class by injecting coverage probes.
     *
     * <p>Returns {@code null} (no transformation) for classes that are filtered out by
     * include/exclude patterns, detected as coming from test output directories, or that
     * cause an unexpected exception during instrumentation.</p>
     *
     * @param loader              the class loader loading the class
     * @param className           the internal name of the class (e.g. {@code org/example/Foo})
     * @param classBeingRedefined the class being redefined, or {@code null}
     * @param protectionDomain    the protection domain of the class
     * @param classfileBuffer     the original class file bytes
     * @return instrumented class bytes, or {@code null} to leave the class unchanged
     */
    @Override
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) {
        if (className == null) {
            LOG.debug("Encountered a nullable className. Skipping.");
            return null;
        }

        if (isFromTestOutput(protectionDomain)) {
            LOG.debug("Encountered a test file {}. Skipping.", className);
            return null;
        }

        if (!classCoverageFilter.shouldInclude(className, null, ClassOrigin.UNKNOWN)) {
            LOG.debug("Encountered a non included file {}. Skipping.", className);
            return null;
        }

        try {
            LOG.debug("Transforming class: {}", className);
            ClassModel classModel = coverageContextResolver.resolveClassModel(className);

            if (classModel != null) {
                LOG.trace("Source map found for {}, using source-aware injector", className);
                return sourceAwareProbeInjector.injectProbes(
                        className, new SourceAwareInput(classfileBuffer, classModel));
            } else {
                LOG.trace("No source map for {}, using default injector", className);
                return defaultProbeInjector.injectProbes(className, classfileBuffer);
            }
        } catch (Exception e) {
            LOG.error("Failed to transform class: {}", className, e);
            return null;
        }
    }

    /**
     * Checks if a class is related to the test context.
     *
     * @param protectionDomain encapsulates the characteristics of a domain
     * @return true if the class is from the test source set.
     */
    private boolean isFromTestOutput(ProtectionDomain protectionDomain) {
        if (protectionDomain == null ||
                protectionDomain.getCodeSource() == null ||
                protectionDomain.getCodeSource().getLocation() == null) {
            return false; // unknown origin -> don't classify as test here
        }

        String location = protectionDomain.getCodeSource().getLocation().toString();

        // Gradle / Maven common test output folders
        return location.contains("/build/classes/java/test/")
                || location.contains("/build/classes/kotlin/test/")
                || location.contains("/build/resources/test/")
                || location.contains("/target/test-classes/")
                || location.contains("/test-classes/")
                // sometimes jars are named like *-tests.jar
                || location.matches(".*[-.]tests?\\.jar!?.*");
    }

}

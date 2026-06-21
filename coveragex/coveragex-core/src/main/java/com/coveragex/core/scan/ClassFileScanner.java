package com.coveragex.core.scan;

import com.coveragex.core.analysis.source.CoverageContextResolver;
import com.coveragex.core.analysis.source.model.ClassModel;
import com.coveragex.core.probe.ProbePlan;
import com.coveragex.core.probe.ProbePlanBuilder;
import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Scans compiled production classes and builds probe plans for enrichment.
 */
public final class ClassFileScanner {

    private final ProbePlanBuilder probePlanBuilder;

    public ClassFileScanner() {
        this(new ProbePlanBuilder());
    }

    public ClassFileScanner(ProbePlanBuilder probePlanBuilder) {
        this.probePlanBuilder = probePlanBuilder;
    }

    public Map<String, ProbePlan> scan(Path classesDir,
                                       ClassCoverageFilter filter,
                                       CoverageContextResolver coverageContextResolver) throws IOException {
        if (classesDir == null || !Files.isDirectory(classesDir)) {
            return Map.of();
        }

        Map<String, ProbePlan> sorted = new TreeMap<>();
        try (Stream<Path> files = Files.walk(classesDir)) {
            Iterator<Path> iterator = files.filter(this::isClassFile).iterator();
            while (iterator.hasNext()) {
                Path classFile = iterator.next();
                byte[] bytes = Files.readAllBytes(classFile);
                ClassReader reader = new ClassReader(bytes);
                String classId = reader.getClassName();
                if (!filter.shouldInclude(classId, classFile, ClassOrigin.PRODUCTION_OUTPUT)) {
                    continue;
                }

                ClassModel classModel = coverageContextResolver != null
                        ? coverageContextResolver.resolveClassModel(classId)
                        : null;
                // Reuse the reader already needed for class identity so ASM does one pass per class.
                ProbePlan plan = probePlanBuilder.build(classId, reader, classModel);
                if (plan.probeCount() == 0 && filter.excludeZeroProbeClasses()) {
                    continue;
                }
                sorted.put(classId, plan);
            }
        }
        return new LinkedHashMap<>(sorted);
    }

    private boolean isClassFile(Path path) {
        return Files.isRegularFile(path) && path.getFileName().toString().endsWith(".class");
    }
}

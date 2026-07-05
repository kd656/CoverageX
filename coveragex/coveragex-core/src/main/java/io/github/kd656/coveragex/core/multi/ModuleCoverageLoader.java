package io.github.kd656.coveragex.core.multi;

import io.github.kd656.coveragex.api.data.ClassCoverage;
import io.github.kd656.coveragex.api.data.ExecutionData;
import io.github.kd656.coveragex.api.io.internal.BinaryDataReader;
import io.github.kd656.coveragex.core.analysis.source.model.SemanticIndex;
import io.github.kd656.coveragex.core.report.ReportInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns each {@link ModuleCoverageDescriptor} into a {@link ReportInput}.
 *
 * <p>Resolution order per descriptor:</p>
 * <ol>
 *   <li>if the descriptor's {@code execFile} exists → read it,</li>
 *   <li>else if the descriptor has compiled classes and a {@code SemanticIndex} →
 *       synthesize zero coverage so DTO-only modules still appear in the report,</li>
 *   <li>else the descriptor is skipped (a warning is logged).</li>
 * </ol>
 */
public final class ModuleCoverageLoader {

    private static final Logger LOG = LoggerFactory.getLogger(ModuleCoverageLoader.class);

    private final SemanticIndexLoader semanticIndexLoader;
    private final BinaryDataReader execReader;

    public ModuleCoverageLoader(SemanticIndexLoader semanticIndexLoader) {
        this(semanticIndexLoader, new BinaryDataReader());
    }

    public ModuleCoverageLoader(SemanticIndexLoader semanticIndexLoader, BinaryDataReader execReader) {
        this.semanticIndexLoader = semanticIndexLoader;
        this.execReader = execReader;
    }

    public List<ReportInput> load(List<ModuleCoverageDescriptor> descriptors) throws IOException {
        List<ReportInput> inputs = new ArrayList<>(descriptors.size());
        for (ModuleCoverageDescriptor descriptor : descriptors) {
            ExecutionData data = tryLoadExec(descriptor);
            if (data == null) {
                data = trySynthesizeZeroCoverage(descriptor);
            }
            if (data == null) {
                LOG.warn("coveragex: no coverage data for module {} (looked for {}). Skipped.",
                        descriptor.displayName(), descriptor.execFile());
                continue;
            }
            inputs.add(new ReportInput(
                    descriptor.scopeId(),
                    descriptor.displayName(),
                    descriptor.sourceDirectory(),
                    data));
        }
        return inputs;
    }

    private ExecutionData tryLoadExec(ModuleCoverageDescriptor descriptor) throws IOException {
        if (descriptor.execFile() == null || !Files.exists(descriptor.execFile())) {
            return null;
        }
        ExecutionData data = execReader.read(descriptor.execFile());
        LOG.debug("coveragex: loaded {} classes from {}", data.classCount(), descriptor.displayName());
        return data;
    }

    private ExecutionData trySynthesizeZeroCoverage(ModuleCoverageDescriptor descriptor) throws IOException {
        if (descriptor.classesDirectory() == null
                || !Files.isDirectory(descriptor.classesDirectory())
                || descriptor.mapFile() == null
                || !Files.exists(descriptor.mapFile())) {
            return null;
        }
        SemanticIndex index = semanticIndexLoader.load(descriptor.mapFile());
        Map<String, ClassCoverage> classes = new LinkedHashMap<>();
        for (String classId : index.getClasses().keySet()) {
            classes.put(classId, ClassCoverage.zeroCoverage(classId, List.of()));
        }
        LOG.info("coveragex: {} has no coveragex.exec but has production classes — "
                + "synthesized zero-coverage entries from its SemanticIndex.",
                descriptor.displayName());
        return new ExecutionData(classes);
    }
}

package io.github.kd656.coveragex.core.analysis.source;

import io.github.kd656.coveragex.core.analysis.source.model.ClassModel;
import io.github.kd656.coveragex.core.analysis.source.model.SemanticIndex;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CoverageContextResolver {

    private final CoverageContextLoader<SemanticIndex> loader = new JacksonCoverageContextLoader();
    private final Path explicitMapPathOrNull;
    private SemanticIndex cachedIndex;
    private boolean loadAttempted;

    public CoverageContextResolver(Path explicitMapPathOrNull) {
        this.explicitMapPathOrNull = explicitMapPathOrNull;
    }

    /**
     * Returns the {@link ClassModel} for the class being transformed, or {@code null} if
     * no map file is configured, the file does not exist, or the class is not present in
     * the index.
     *
     * <p>When {@code null} is returned the caller should fall back to default (opcode-based)
     * probe injection.</p>
     *
     * @param classInternalName JVM internal class name (slash-notation, e.g.
     *                          {@code com/example/MyClass})
     * @return the matching {@link ClassModel}, or {@code null}
     */
    public ClassModel resolveClassModel(String classInternalName) {
        if (explicitMapPathOrNull == null) {
            return null;
        }

        if (!Files.isRegularFile(explicitMapPathOrNull)) {
            return null;
        }

        SemanticIndex index = loadIndexOrNull();
        if (index == null) {
            return null;
        }

        // Preferred: map keys are internal names (org/example/Foo)
        ClassModel cm = index.getClasses().get(classInternalName);
        if (cm != null) {
            return cm;
        }

        // Fallback: map keys are dotted names (org.example.Foo)
        return index.getClasses().get(classInternalName.replace('/', '.'));
    }

    private SemanticIndex loadIndexOrNull() {
        if (loadAttempted) {
            return cachedIndex;
        }

        loadAttempted = true;
        try {
            cachedIndex = this.loader.load(explicitMapPathOrNull);
        } catch (IOException e) {
            cachedIndex = null;
        }
        return cachedIndex;
    }
}

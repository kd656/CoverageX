package io.github.kd656.coveragex.core.multi;

import io.github.kd656.coveragex.core.analysis.source.model.ClassModel;
import io.github.kd656.coveragex.core.analysis.source.model.MethodModel;
import io.github.kd656.coveragex.core.analysis.source.model.MethodReference;
import io.github.kd656.coveragex.core.analysis.source.model.SemanticIndex;

/**
 * Unions several {@link SemanticIndex} instances into one.
 *
 * <p>Used by the Maven plugin at {@code analyze} time to produce the combined map
 * the agent reads at instrumentation time: without it, {@code prepare-agent} on
 * a module only knows about that module's own source-derived condition text, and
 * classes from upstream reactor modules loaded via the classpath fall back to
 * generic opcode labels like {@code "if (a < b)"}.</p>
 */
public final class SemanticIndexMerger {

    private SemanticIndexMerger() {}

    /**
     * Folds {@code source} into {@code destination}, keeping {@code destination}'s
     * entries on collision (local-wins). Classes present only in {@code source}
     * are cloned into {@code destination} — including every method — so the
     * resulting index is self-contained and safe to serialize.
     *
     * <p>The name states the merge policy explicitly so callers cannot mis-order
     * arguments and silently invert local-wins to upstream-wins.</p>
     *
     * @param destination the target index, mutated in place
     * @param source      the index to fold in
     */
    public static void mergePreservingFirst(SemanticIndex destination, SemanticIndex source) {
        for (var entry : source.getClasses().entrySet()) {
            String classId = entry.getKey();
            ClassModel sourceClass = entry.getValue();
            if (destination.getClasses().containsKey(classId)) {
                continue;
            }
            ClassModel destClass = destination.getOrCreateClass(classId, sourceClass.getSourceFile());
            for (var methodEntry : sourceClass.getMethods().entrySet()) {
                MethodReference ref = methodEntry.getKey();
                MethodModel model = methodEntry.getValue();
                destClass.getOrCreate(ref, () -> model);
            }
        }
    }
}

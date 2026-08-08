package io.github.kd656.coveragex.core.multi;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Build-neutral helpers for turning build-tool module identities into stable
 * {@code scopeId}s. Shared by every {@link ModuleCoverageDiscoverer} implementation
 * (and the single-project report paths) so the sanitization and collision rules stay
 * identical across build tools.
 */
public final class Scopes {

    private Scopes() {}

    /**
     * Normalizes a raw module/artifact/project name into a filesystem- and DOM-safe
     * identifier: unsafe characters become dashes, runs of dashes collapse, and
     * leading/trailing dashes are trimmed. Blank or dash-only input yields
     * {@code fallback}.
     */
    public static String sanitize(String raw, String fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String replaced = raw.replaceAll("[^A-Za-z0-9._-]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-+|-+$", "");
        return replaced.isBlank() ? fallback : replaced;
    }

    /** Relativizes {@code child} against {@code root}, falling back to the file name. */
    public static Path safeRelativize(Path root, Path child) {
        try {
            return root.relativize(child);
        } catch (IllegalArgumentException e) {
            return child.getFileName();
        }
    }

    /**
     * Appends {@code -2}, {@code -3}, ... to duplicate {@code scopeId}s. Deterministic
     * because it preserves the input iteration order (typically reactor/subproject order).
     */
    public static List<ModuleCoverageDescriptor> deduplicateScopeIds(
            List<ModuleCoverageDescriptor> raw) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        List<ModuleCoverageDescriptor> assigned = new ArrayList<>(raw.size());
        for (ModuleCoverageDescriptor d : raw) {
            String base = d.scopeId();
            int seen = counts.getOrDefault(base, 0) + 1;
            counts.put(base, seen);
            if (seen == 1) {
                assigned.add(d);
            } else {
                assigned.add(withScopeId(d, base + "-" + seen));
            }
        }
        return assigned;
    }

    private static ModuleCoverageDescriptor withScopeId(ModuleCoverageDescriptor d, String scopeId) {
        return new ModuleCoverageDescriptor(
                scopeId,
                d.displayName(),
                d.baseDirectory(),
                d.relativePath(),
                d.execFile(),
                d.mapFile(),
                d.sourceDirectory(),
                d.classesDirectory());
    }
}

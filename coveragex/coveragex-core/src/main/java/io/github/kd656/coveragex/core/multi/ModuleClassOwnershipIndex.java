package io.github.kd656.coveragex.core.multi;

import io.github.kd656.coveragex.api.data.ClassCoverage;
import io.github.kd656.coveragex.api.data.ExecutionData;
import io.github.kd656.coveragex.core.analysis.source.model.SemanticIndex;
import io.github.kd656.coveragex.core.report.ReportInput;
import io.github.kd656.coveragex.core.scan.ClassCoverageFilter;
import io.github.kd656.coveragex.core.scan.ClassOrigin;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Maps FQCN → owning {@code scopeId} by reading each module's {@code coveragex.map.json}.
 *
 * <p>First build-order occurrence wins on collision: if two modules' {@code SemanticIndex}s
 * list the same FQCN, whichever module the discoverer visited first owns it. The router
 * then reassigns coverage for that FQCN to the owner, and — if both modules also contributed
 * coverage bytes for it — the subsequent {@link io.github.kd656.coveragex.api.data.ExecutionData}
 * merge surfaces the conflict via {@code DuplicateClassCoverageException}.</p>
 *
 * <p>Per-module {@code prepare-agent} filters do <b>not</b> influence ownership. Only the
 * aggregate-level {@link ClassCoverageFilter} passed in here does.</p>
 */
public final class ModuleClassOwnershipIndex {

    private final Map<String, String> scopeIdByClassId;

    /** Package-private for tests that need to inject a synthesized ownership map. */
    ModuleClassOwnershipIndex(Map<String, String> scopeIdByClassId) {
        this.scopeIdByClassId = Map.copyOf(scopeIdByClassId);
    }

    /**
     * Builds an ownership index by reading each descriptor's {@code mapFile}.
     * Descriptors without a map file (either because {@code analyze} did not run or
     * because the module has no Java code) are silently skipped — those modules
     * simply do not own any classes.
     */
    public static ModuleClassOwnershipIndex build(
            List<ModuleCoverageDescriptor> descriptors,
            ClassCoverageFilter aggregateFilter,
            SemanticIndexLoader loader) throws IOException {
        Map<String, String> ownership = new LinkedHashMap<>();
        for (ModuleCoverageDescriptor descriptor : descriptors) {
            if (descriptor.mapFile() == null || !Files.exists(descriptor.mapFile())) {
                continue;
            }
            SemanticIndex index = loader.load(descriptor.mapFile());
            for (String classId : index.getClasses().keySet()) {
                if (aggregateFilter != null
                        && !aggregateFilter.shouldInclude(classId, null, ClassOrigin.PRODUCTION_OUTPUT)) {
                    continue;
                }
                ownership.putIfAbsent(classId, descriptor.scopeId());
            }
        }
        return new ModuleClassOwnershipIndex(ownership);
    }

    /** Empty index — no owners. Used by callers that opt out of ownership resolution. */
    public static ModuleClassOwnershipIndex empty() {
        return new ModuleClassOwnershipIndex(Map.of());
    }

    public Optional<String> ownerOf(String classId) {
        return Optional.ofNullable(scopeIdByClassId.get(classId));
    }

    public Set<String> knownClasses() {
        return scopeIdByClassId.keySet();
    }

    public int size() {
        return scopeIdByClassId.size();
    }

    /**
     * Rebuckets each raw input's coverage entries into the owning module's
     * {@link ReportInput}.
     *
     * <p>Example: {@code service}'s test JVM hits {@code dto.UserRecord} because
     * that class is loaded during {@code service}'s tests. Ownership moves the
     * entry into {@code dto}'s bucket. If two modules both recorded coverage for
     * the same class, entries are merged via {@link ClassCoverage#merge}.</p>
     *
     * <p>Fallback: a class whose owner is unknown, or whose owner is not among
     * the input scopes, stays with the input that produced it. This keeps
     * generated / runtime-only classes visible even without a {@link SemanticIndex}
     * entry.</p>
     */
    public List<ReportInput> route(List<ReportInput> rawInputs) {
        Map<String, Map<String, ClassCoverage>> bucketsByScope = new LinkedHashMap<>();
        for (ReportInput input : rawInputs) {
            bucketsByScope.put(input.scopeId(), new LinkedHashMap<>());
        }

        for (ReportInput input : rawInputs) {
            String sourceScope = input.scopeId();
            for (Map.Entry<String, ClassCoverage> entry : input.executionData().classes().entrySet()) {
                String classId = entry.getKey();
                ClassCoverage coverage = entry.getValue();

                String owner = ownerOf(classId).orElse(sourceScope);
                if (!bucketsByScope.containsKey(owner)) {
                    owner = sourceScope;
                }

                Map<String, ClassCoverage> destination = bucketsByScope.get(owner);
                destination.compute(classId, (k, existing) ->
                        existing == null ? coverage : ClassCoverage.merge(existing, coverage));
            }
        }

        List<ReportInput> routed = new ArrayList<>(rawInputs.size());
        for (ReportInput input : rawInputs) {
            Map<String, ClassCoverage> bucket = bucketsByScope.get(input.scopeId());
            routed.add(new ReportInput(
                    input.scopeId(),
                    input.displayName(),
                    input.sourceDirectory(),
                    new ExecutionData(bucket),
                    input.sourceFilesByClassId()
            ));
        }
        return routed;
    }
}

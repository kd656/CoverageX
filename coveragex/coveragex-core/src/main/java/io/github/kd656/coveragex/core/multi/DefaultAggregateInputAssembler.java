package io.github.kd656.coveragex.core.multi;

import io.github.kd656.coveragex.core.report.ReportInput;
import io.github.kd656.coveragex.core.scan.ClassCoverageFilter;

import java.io.IOException;
import java.util.List;

/**
 * Build-tool-neutral assembler: discover → load → build ownership → route.
 *
 * <p>Every collaborator is an interface or a pure core class, so the Maven mojos and Gradle
 * tasks share it, each supplying only their own {@link ModuleCoverageDiscoverer}.</p>
 */
public final class DefaultAggregateInputAssembler implements AggregateInputAssembler {

    private final ModuleCoverageDiscoverer discoverer;
    private final ModuleCoverageLoader loader;
    private final SemanticIndexLoader semanticIndexLoader;
    private final ClassCoverageFilter aggregateFilter;

    public DefaultAggregateInputAssembler(ModuleCoverageDiscoverer discoverer,
                                           ModuleCoverageLoader loader,
                                           SemanticIndexLoader semanticIndexLoader,
                                           ClassCoverageFilter aggregateFilter) {
        this.discoverer = discoverer;
        this.loader = loader;
        this.semanticIndexLoader = semanticIndexLoader;
        this.aggregateFilter = aggregateFilter;
    }

    /**
     * Builds an assembler with the standard collaborators (one shared {@link SemanticIndexLoader}
     * and an include/exclude {@link ClassCoverageFilter}) so callers don't repeat the wiring.
     *
     * @param includes may be {@code null} (treated as empty)
     * @param excludes may be {@code null} (treated as empty)
     */
    public static DefaultAggregateInputAssembler create(ModuleCoverageDiscoverer discoverer,
                                                        List<String> includes,
                                                        List<String> excludes) {
        SemanticIndexLoader semanticIndexLoader = new SemanticIndexLoader();
        ModuleCoverageLoader loader = new ModuleCoverageLoader(semanticIndexLoader);
        ClassCoverageFilter filter = new ClassCoverageFilter(
                includes != null ? includes : List.of(),
                excludes != null ? excludes : List.of());
        return new DefaultAggregateInputAssembler(discoverer, loader, semanticIndexLoader, filter);
    }

    @Override
    public List<ReportInput> assemble() throws IOException {
        List<ModuleCoverageDescriptor> descriptors = discoverer.discover();
        List<ReportInput> rawInputs = loader.load(descriptors);
        ModuleClassOwnershipIndex ownership = ModuleClassOwnershipIndex.build(
                descriptors, aggregateFilter, semanticIndexLoader);
        return ownership.route(rawInputs);
    }
}

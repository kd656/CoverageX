package io.github.kd656.coveragex.core.multi;

import io.github.kd656.coveragex.core.report.ReportInput;
import io.github.kd656.coveragex.core.scan.ClassCoverageFilter;

import java.io.IOException;
import java.util.List;

/**
 * Build-tool-neutral assembler: discover → load → build ownership → route.
 *
 * <p>Every collaborator is an interface or a pure core class, so a Maven mojo
 * and a future Gradle task instantiate this same class with only their own
 * {@link ModuleCoverageDiscoverer} implementation.</p>
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

    @Override
    public List<ReportInput> assemble() throws IOException {
        List<ModuleCoverageDescriptor> descriptors = discoverer.discover();
        List<ReportInput> rawInputs = loader.load(descriptors);
        ModuleClassOwnershipIndex ownership = ModuleClassOwnershipIndex.build(
                descriptors, aggregateFilter, semanticIndexLoader);
        return ownership.route(rawInputs);
    }
}

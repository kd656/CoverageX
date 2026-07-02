package io.github.kd656.coveragex.core.multi;

import io.github.kd656.coveragex.core.report.ReportInput;

import java.io.IOException;
import java.util.List;

/**
 * Composes discovery, loading, ownership resolution, and routing into a single
 * operation that returns the routed inputs ready for reporting.
 */
public interface AggregateInputAssembler {

    List<ReportInput> assemble() throws IOException;
}

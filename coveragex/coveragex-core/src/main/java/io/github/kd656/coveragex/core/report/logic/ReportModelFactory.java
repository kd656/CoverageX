package io.github.kd656.coveragex.core.report.logic;

import io.github.kd656.coveragex.api.data.ExecutionData;
import io.github.kd656.coveragex.core.report.ReportInput;
import io.github.kd656.coveragex.core.report.model.ReportModel;

import java.util.List;

/**
 * Produces a {@link ReportModel} from raw {@link ExecutionData} or from one or more
 * {@link ReportInput}s.
 *
 * <p>Extracting the model-creation step from {@code ReportingService} lets aggregation
 * flows build a scoped model from many inputs while single-module callers keep the
 * one-shot path.</p>
 */
public interface ReportModelFactory {

    /** Builds a single-scope model from raw execution data. */
    ReportModel build(ExecutionData data);

    /**
     * Builds a scoped model from one or more inputs. A one-element list produces a
     * single-scope model whose scope carries the input's identity; a multi-element
     * list produces one scope per input plus an aggregate {@code SummaryMetrics}.
     */
    ReportModel build(List<ReportInput> inputs);
}

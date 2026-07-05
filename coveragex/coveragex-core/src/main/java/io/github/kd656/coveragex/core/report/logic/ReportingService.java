package io.github.kd656.coveragex.core.report.logic;

import io.github.kd656.coveragex.api.data.ExecutionData;
import io.github.kd656.coveragex.core.report.ReportConfig;
import io.github.kd656.coveragex.core.report.ReportInput;
import io.github.kd656.coveragex.core.report.ReportRenderer;
import io.github.kd656.coveragex.core.report.model.ReportModel;
import io.github.kd656.coveragex.core.report.model.ReportingType;
import io.github.kd656.coveragex.core.report.views.HtmlReportRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Orchestrates report generation: builds a {@link ReportModel} via the injected
 * {@link ReportModelFactory}, runs the {@link ReportPipeline} over it, then hands
 * the model to each configured {@link ReportRenderer}.
 *
 * <p>The model-building and pipeline-running logic used to live here as private
 * methods. They now live in {@link DefaultReportModelFactory} and {@link ReportPipeline}
 * so aggregated reporting can reuse them without duplication.</p>
 */
public class ReportingService {

    private static final Logger LOG = LoggerFactory.getLogger(ReportingService.class);

    private final ReportModelFactory modelFactory;
    private final ReportPipeline pipeline;
    private final Map<ReportingType, ReportRenderer> views;

    public ReportingService() {
        this(
                new DefaultReportModelFactory(),
                new ReportPipeline(),
                Map.of(ReportingType.HTML, new HtmlReportRenderer())
        );
    }

    public ReportingService(Map<ReportingType, ReportRenderer> views) {
        this(new DefaultReportModelFactory(), new ReportPipeline(), views);
    }

    public ReportingService(ReportModelFactory modelFactory,
                             ReportPipeline pipeline,
                             Map<ReportingType, ReportRenderer> views) {
        this.modelFactory = modelFactory;
        this.pipeline = pipeline;
        this.views = views;
    }

    public void report(ReportConfig config, ExecutionData data) {
        ReportModel model = modelFactory.build(data);
        pipeline.run(config, model);
        renderViews(config, model);
    }

    /**
     * Multi-input entry point for aggregated reporting.
     *
     * <p>Builds one scoped {@link ReportModel} spanning every input, runs the
     * pipeline once over the whole model, then hands it to each view. The pipeline
     * sees the whole build — it does not run once per module. Downstream view
     * behavior (flat vs. module-grouped HTML layout) is driven by
     * {@link ReportModel#isScoped()}.</p>
     *
     * <p>A one-element list is legal and produces the same shape as
     * {@link #report(ReportConfig, ExecutionData)} once the input's scope id
     * matches the single-module sentinel; otherwise it produces a scoped model
     * with one scope.</p>
     */
    public void report(ReportConfig config, List<ReportInput> inputs) {
        ReportModel model = modelFactory.build(inputs);
        pipeline.run(config, model);
        renderViews(config, model);
    }

    private void renderViews(ReportConfig config, ReportModel model) {
        for (ReportingType format : config.reportFormats()) {
            ReportRenderer view = views.get(format);
            if (view == null) {
                LOG.warn("No view registered for format '{}' — skipping.", format);
            } else {
                view.render(model, config.context());
            }
        }
    }
}

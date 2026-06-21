package io.github.kd656.coveragex.core.report;

import io.github.kd656.coveragex.core.report.model.ReportModel;
import io.github.kd656.coveragex.core.report.model.ReportingType;

public interface ReportView {
    void render(ReportModel model, ReportContext context);
    ReportingType type();
}

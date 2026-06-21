package com.coveragex.core.report;

import com.coveragex.core.report.model.ReportModel;
import com.coveragex.core.report.model.ReportingType;

public interface ReportView {
    void render(ReportModel model, ReportContext context);
    ReportingType type();
}

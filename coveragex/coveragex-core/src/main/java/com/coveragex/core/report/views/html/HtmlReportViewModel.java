package com.coveragex.core.report.views.html;

import java.util.List;

public record HtmlReportViewModel(HtmlSummary topBar, List<HtmlNavNode> navTree) {}

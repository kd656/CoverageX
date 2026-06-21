package com.coveragex.core.report.pipeline.results;

import com.coveragex.core.insights.Insight;
import com.coveragex.core.report.model.StepResult;
import java.util.List;

public record InsightsResult(List<Insight> insights) implements StepResult {}

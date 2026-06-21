package com.coveragex.core.report.pipeline.results;

import com.coveragex.core.report.model.StepResult;
import java.util.List;

public record InvocationResult(List<InvocationReport> reports) implements StepResult {}

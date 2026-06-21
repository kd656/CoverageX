package com.coveragex.core.report.pipeline.results;

import com.coveragex.core.report.model.StepResult;
import java.util.List;

public record SuggestionsResult(List<Suggestion> suggestions) implements StepResult {}

package io.github.kd656.coveragex.core.report.pipeline.results;

import io.github.kd656.coveragex.core.report.model.StepResult;
import java.util.List;

public record SuggestionsResult(List<Suggestion> suggestions) implements StepResult {}

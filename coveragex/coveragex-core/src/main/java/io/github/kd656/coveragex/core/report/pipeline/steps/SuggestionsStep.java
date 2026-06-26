package io.github.kd656.coveragex.core.report.pipeline.steps;

import io.github.kd656.coveragex.core.report.model.BranchResult;
import io.github.kd656.coveragex.core.report.model.ClassMetrics;
import io.github.kd656.coveragex.core.report.model.ConditionCase;
import io.github.kd656.coveragex.core.report.model.MethodMetrics;
import io.github.kd656.coveragex.core.report.model.ReportModel;
import io.github.kd656.coveragex.core.report.pipeline.PipelineStepId;
import io.github.kd656.coveragex.core.report.pipeline.ReportPipelineStep;
import io.github.kd656.coveragex.core.report.pipeline.results.Suggestion;
import io.github.kd656.coveragex.core.report.pipeline.results.SuggestionsResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SuggestionsStep extends ReportPipelineStep {

    @Override
    public PipelineStepId stepId() {
        return PipelineStepId.SUGGESTIONS;
    }

    @Override
    public void handle(ReportModel model) {
        List<Suggestion> suggestions = new ArrayList<>();

        for (ClassMetrics cm : model.getClassMetrics()) {
            Map<String, List<BranchResult>> branchesByMethod = cm.branches().stream()
                .collect(Collectors.groupingBy(BranchResult::methodName));

            for (MethodMetrics mm : cm.methods()) {
                if (mm.isImplicitDefaultConstructor()) {
                    continue;
                }

                // Method never invoked
                if (mm.hitCount() == 0) {
                    suggestions.add(new Suggestion(cm.classId(), mm.methodName(), mm.startLine(),
                        mm.isConstructor()
                            ? "Add a basic test that creates an instance and exercises this constructor."
                            : "Add a basic happy-path test that calls " + mm.methodName() + "()."));
                }

                // Branch with one direction uncovered — one suggestion per ConditionCase
                for (BranchResult br : branchesByMethod.getOrDefault(mm.methodName(), List.of())) {
                    for (ConditionCase cc : br.conditions()) {
                        boolean trueHit  = cc.trueDirection().hit();
                        boolean falseHit = cc.falseDirection().hit();
                        String condText  = cc.conditionText();

                        if (trueHit && !falseHit) {
                            suggestions.add(new Suggestion(cm.classId(), mm.methodName(), br.line(),
                                "Add a test that makes '" + condText + "' evaluate to false."));
                        } else if (!trueHit && falseHit) {
                            suggestions.add(new Suggestion(cm.classId(), mm.methodName(), br.line(),
                                "Add a test that makes '" + condText + "' evaluate to true."));
                        }
                    }
                }

                // Low argument diversity
                int totalInvocations = mm.hitCount();
                int uniqueCombos = mm.invocations().size();
                if (uniqueCombos >= 1 && uniqueCombos <= 2 && totalInvocations > 5) {
                    suggestions.add(new Suggestion(cm.classId(), mm.methodName(), mm.startLine(),
                        "Consider parameterised or property-based testing for " + displayName(cm, mm)
                            + "() — currently only " + uniqueCombos + " unique argument combination(s) used."));
                }
            }
        }

        model.put(SuggestionsResult.class, new SuggestionsResult(List.copyOf(suggestions)));
        proceed(model);
    }

    private String displayName(ClassMetrics cm, MethodMetrics mm) {
        return mm.isConstructor() ? cm.simpleName() : mm.methodName();
    }
}

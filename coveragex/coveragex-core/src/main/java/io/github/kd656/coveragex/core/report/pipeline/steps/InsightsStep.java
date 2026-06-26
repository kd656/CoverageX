package io.github.kd656.coveragex.core.report.pipeline.steps;

import io.github.kd656.coveragex.core.insights.Insight;
import io.github.kd656.coveragex.core.insights.Severity;
import io.github.kd656.coveragex.core.report.model.BranchResult;
import io.github.kd656.coveragex.core.report.model.ClassMetrics;
import io.github.kd656.coveragex.core.report.model.ConditionCase;
import io.github.kd656.coveragex.core.report.model.Coverage;
import io.github.kd656.coveragex.core.report.model.MethodMetrics;
import io.github.kd656.coveragex.core.report.model.ReportModel;
import io.github.kd656.coveragex.core.report.pipeline.PipelineStepId;
import io.github.kd656.coveragex.core.report.pipeline.ReportPipelineStep;
import io.github.kd656.coveragex.core.report.pipeline.results.InsightsResult;

import java.util.*;
import java.util.stream.Collectors;

// TODO: Fix hardcode strings and move it to a separate class including descriptions (maybe create an utils class)
public class InsightsStep extends ReportPipelineStep {

    @Override
    public PipelineStepId stepId() {
        return PipelineStepId.INSIGHTS;
    }

    @Override
    public void handle(ReportModel model) {
        List<Insight> insights = new ArrayList<>();

        for (ClassMetrics cm : model.getClassMetrics()) {
            insights.addAll(analyzeClass(cm));
        }

        model.put(InsightsResult.class, new InsightsResult(List.copyOf(insights)));
        proceed(model);
    }

    private List<Insight> analyzeClass(ClassMetrics cm) {
        List<Insight> insights = new ArrayList<>();

        // DEAD_CLASS: all executable lines are MISS
        boolean hasExecutable = cm.lines().stream().anyMatch(ls -> ls.coverage() != Coverage.NOT_EXECUTABLE);
        boolean allMiss = hasExecutable && cm.lines().stream()
            .filter(ls -> ls.coverage() != Coverage.NOT_EXECUTABLE)
            .allMatch(ls -> ls.coverage() == Coverage.MISS);

        if (allMiss) {
            insights.add(new Insight(cm.classId(), null, -1,
                "DEAD_CLASS", Severity.CRITICAL,
                "Class never loaded during tests",
                "No probe in this class was ever hit. The class has no test coverage at all."));
        }

        // Group branches by method
        Map<String, List<BranchResult>> branchesByMethod = cm.branches().stream()
            .collect(Collectors.groupingBy(BranchResult::methodName));

        for (MethodMetrics mm : cm.methods()) {
            if (mm.isImplicitDefaultConstructor()) {
                continue;
            }
            List<Insight> methodInsights = analyzeMethod(cm.classId(), mm,
                branchesByMethod.getOrDefault(mm.methodName(), List.of()));
            insights.addAll(methodInsights);
        }

        insights.sort(Comparator.comparingInt(Insight::line).thenComparingInt(i -> i.severity().ordinal()));
        return insights;
    }

    private List<Insight> analyzeMethod(String classId, MethodMetrics methodMetrics,
                                         List<BranchResult> branches) {
        List<Insight> insights = new ArrayList<>();

        boolean methodHit = methodMetrics.hitCount() > 0;

        // DEAD_METHOD
        if (!methodHit) {
            insights.add(new Insight(classId, methodMetrics.methodName(), methodMetrics.startLine(),
                "DEAD_METHOD", Severity.CRITICAL,
                methodMetrics.isConstructor() ? "Constructor never executed" : "Method never executed",
                methodMetrics.isConstructor()
                    ? "Add a test that creates an instance to exercise this constructor."
                    : "Add a test that calls a method " + methodMetrics.methodName() + "() with representative arguments."));
        }

        // Branch-level insights — one insight per ConditionCase
        for (BranchResult br : branches) {
            for (ConditionCase cc : br.conditions()) {
                boolean trueHit  = cc.trueDirection().hit();
                boolean falseHit = cc.falseDirection().hit();
                String condText  = cc.conditionText();

                if (!trueHit && !falseHit) {
                    insights.add(new Insight(classId, methodMetrics.methodName(), br.line(),
                        "ZERO_BRANCH_COVERAGE", Severity.CRITICAL,
                        "Branch never exercised in either direction",
                        "Add tests that make this condition both true and false."));
                } else if (trueHit && !falseHit) {
                    insights.add(new Insight(classId, methodMetrics.methodName(), br.line(),
                        "MISSING_BRANCH_FALSE", Severity.WARNING,
                        "FALSE branch never taken",
                        "Add a test that makes this condition false."));
                    addPathInsight(insights, classId, methodMetrics.methodName(), br.line(), condText, false);
                } else if (!trueHit && falseHit) {
                    insights.add(new Insight(classId, methodMetrics.methodName(), br.line(),
                        "MISSING_BRANCH_TRUE", Severity.WARNING,
                        "TRUE branch never taken",
                        "Add a test that makes this condition true."));
                    addPathInsight(insights, classId, methodMetrics.methodName(), br.line(), condText, true);
                }
            }
        }

        int totalInvocations = methodMetrics.hitCount();
        int uniqueArgCombos  = methodMetrics.invocations().size();

        // HIGH_COMPLEXITY_LOW_COVERAGE
        if (methodMetrics.branchProbeCount() >= 4) {
            double branchCoverage = methodMetrics.branchProbeCount() > 0
                ? (100.0 * methodMetrics.hitBranchProbeCount() / methodMetrics.branchProbeCount()) : 0.0;
            if (branchCoverage < 50.0) {
                insights.add(new Insight(classId, methodMetrics.methodName(), methodMetrics.startLine(),
                    "HIGH_COMPLEXITY_LOW_COVERAGE", Severity.WARNING,
                    "Complex " + callableKind(methodMetrics) + " with low branch coverage",
                    "This " + callableKind(methodMetrics) + " has many branches; prioritise adding tests to cover the untested paths."));
            }
        }

        // MONOTONE_TEST
        if (totalInvocations >= 10 && uniqueArgCombos == 1) {
            insights.add(new Insight(classId, methodMetrics.methodName(), methodMetrics.startLine(),
                "MONOTONE_TEST", Severity.WARNING,
                callableTitle(methodMetrics) + " tested repeatedly with identical arguments",
                "Consider consolidating redundant test cases or varying argument combinations."));
        }

        // SINGLE_INVOCATION
        if (methodHit && totalInvocations == 1) {
            insights.add(new Insight(classId, methodMetrics.methodName(), methodMetrics.startLine(),
                "SINGLE_INVOCATION", Severity.INFO,
                callableTitle(methodMetrics) + " exercised only once",
                "Only one code path has been exercised; consider adding tests with varied inputs."));
        }

        // OVER_TESTED
        boolean fullProbeCoverage = methodMetrics.probeCount() > 0
            && methodMetrics.hitProbeCount() == methodMetrics.probeCount();
        if (fullProbeCoverage && totalInvocations > 50 && uniqueArgCombos <= 2) {
            insights.add(new Insight(classId, methodMetrics.methodName(), methodMetrics.startLine(),
                "OVER_TESTED", Severity.INFO,
                "Possibly over-tested — many calls with same arguments",
                "High call count with very few argument combinations may indicate redundant test cases."));
        }

        // TRIVIAL_HEAVY_TESTING
        if (methodMetrics.probeCount() == 1 && totalInvocations > 20) {
            insights.add(new Insight(classId, methodMetrics.methodName(), methodMetrics.startLine(),
                "TRIVIAL_HEAVY_TESTING", Severity.INFO,
                "Trivial " + callableKind(methodMetrics) + " invoked excessively in tests",
                "This " + callableKind(methodMetrics) + " has no branches; the many invocations are likely redundant."));
        }

        // FULL_BRANCH_COVERAGE
        boolean allBranchesCovered = !branches.isEmpty() && branches.stream()
            .flatMap(br -> br.conditions().stream())
            .allMatch(cc -> cc.trueDirection().hit() && cc.falseDirection().hit());
        if (allBranchesCovered) {
            insights.add(new Insight(classId, methodMetrics.methodName(), methodMetrics.startLine(),
                "FULL_BRANCH_COVERAGE", Severity.POSITIVE,
                "Full branch coverage",
                "All branch directions are covered by tests."));
        }

        // DIVERSE_INVOCATIONS
        boolean diverseInvocations = uniqueArgCombos >= 3;
        if (diverseInvocations) {
            insights.add(new Insight(classId, methodMetrics.methodName(), methodMetrics.startLine(),
                "DIVERSE_INVOCATIONS", Severity.POSITIVE,
                "Good test variety — multiple argument paths exercised",
                "Testing with at least 3 distinct argument combinations is a good sign."));
        }

        // OPTIMAL
        boolean hasWarningOrCritical = insights.stream()
            .anyMatch(i -> i.severity() == Severity.CRITICAL || i.severity() == Severity.WARNING);
        if (fullProbeCoverage && allBranchesCovered && diverseInvocations && !hasWarningOrCritical) {
            insights.add(new Insight(classId, methodMetrics.methodName(), methodMetrics.startLine(),
                "OPTIMAL", Severity.POSITIVE,
                "Optimal coverage",
                "100% probe coverage, full branch coverage, and diverse invocations. No action needed."));
        }

        return insights;
    }

    private void addPathInsight(List<Insight> insights, String classId, String methodName,
                                 int line, String condText, boolean missedTrue) {
        if (condText == null) return;
        if (condText.contains("null")) {
            insights.add(new Insight(classId, methodName, line,
                "UNTESTED_NULL_PATH", Severity.WARNING,
                "Null path not covered",
                missedTrue ? "Pass a null argument in a test to exercise this path."
                           : "Add a test with a non-null argument."));
        } else if (condText.contains("isEmpty()") || condText.contains("size() == 0")) {
            insights.add(new Insight(classId, methodName, line,
                "UNTESTED_EMPTY_PATH", Severity.WARNING,
                "Empty collection path not covered",
                "Add a test with a non-empty collection."));
        }
    }

    private String callableKind(MethodMetrics methodMetrics) {
        return methodMetrics.isConstructor() ? "constructor" : "method";
    }

    private String callableTitle(MethodMetrics methodMetrics) {
        return methodMetrics.isConstructor() ? "constructor" : "Method";
    }
}

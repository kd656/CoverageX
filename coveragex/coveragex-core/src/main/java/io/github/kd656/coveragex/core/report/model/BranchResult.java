package io.github.kd656.coveragex.core.report.model;

public record BranchResult(
    String methodName,
    int line,
    String conditionText,
    boolean trueHit,
    boolean falseHit,
    int trueCount,
    int falseCount
) {}

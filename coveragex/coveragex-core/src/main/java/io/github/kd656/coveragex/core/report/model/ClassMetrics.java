package io.github.kd656.coveragex.core.report.model;

import io.github.kd656.coveragex.api.data.ProbeMetadata;
import io.github.kd656.coveragex.api.data.ClassTestCoverage;
import java.util.List;

public record ClassMetrics(
    String classId,
    String simpleName,
    String packageName,
    double lineCoveragePercent,
    double branchCoveragePercent,
    double methodCoveragePercent,
    List<MethodMetrics> methods,
    List<BranchResult> branches,
    List<LineStatus> lines,
    List<ProbeMetadata> probeMetadata,
    ClassTestCoverage testAttribution
) {}

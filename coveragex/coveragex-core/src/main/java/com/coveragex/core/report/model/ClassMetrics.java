package com.coveragex.core.report.model;

import com.coveragex.api.data.ProbeMetadata;
import com.coveragex.api.data.ClassTestCoverage;
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

package io.github.kd656.coveragex.core.report.model;

import io.github.kd656.coveragex.api.data.ProbeMetadata;
import io.github.kd656.coveragex.api.data.ClassTestCoverage;
import java.util.Collection;
import java.util.List;

public record ClassMetrics(
    String classId,
    String sourceFile,
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
) {

    /**
     * Probe-weighted coverage aggregate: {@code executed / total} across every
     * probe of every method in the given classes. Same formula everywhere —
     * top bar, folder rollups, scoped module rollups — so all "%" pills on a
     * report agree on what they mean.
     */
    public static double aggregateProbeCoverage(Collection<ClassMetrics> classes) {
        long total = 0, covered = 0;
        for (ClassMetrics cm : classes) {
            for (MethodMetrics mm : cm.methods()) {
                total   += mm.probeCount();
                covered += mm.hitProbeCount();
            }
        }
        return total > 0 ? (100.0 * covered / total) : 0.0;
    }
}

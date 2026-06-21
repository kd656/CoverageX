package com.coveragex.compat.testutil;

import com.coveragex.api.data.ProbeMetadata;
import com.coveragex.api.data.ProbeMetadata.BranchProbe;
import com.coveragex.api.data.ProbeMetadata.MethodProbe;
import com.coveragex.api.data.ProbeMetadata.ReturnProbe;
import com.coveragex.api.data.ProbeMetadata.SegmentProbe;
import com.coveragex.api.data.ProbeMetadata.ThrowProbe;
import com.coveragex.core.probe.ProbePlan;

import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * Renders a {@link ProbePlan} as a deterministic line-oriented text dump.
 *
 * <p>Used by {@link PlanDumpOnFailure} to write a triage artefact when a contract
 * assertion fails. Sorts by tag → line → probeId so output is stable across runs.</p>
 */
public final class PlanFormatter {

    public static String format(ProbePlan plan) {
        return plan.metadata().stream()
                .sorted(Comparator
                        .comparing(ProbeMetadata::tag)
                        .thenComparingInt(ProbeMetadata::lineNumber)
                        .thenComparingInt(ProbeMetadata::probeId))
                .map(PlanFormatter::line)
                .collect(Collectors.joining("\n"));
    }

    private static String line(ProbeMetadata m) {
        return switch (m) {
            case MethodProbe p  -> "METHOD  | line=%-4d | name=%s".formatted(p.startLine(), p.methodName());
            case BranchProbe p  -> "BRANCH  | line=%-4d | dir=%-5s | text=%s".formatted(p.line(), p.direction(), p.conditionText());
            case ReturnProbe p  -> "RETURN  | line=%-4d | name=%s".formatted(p.line(), p.methodName());
            case ThrowProbe p   -> "THROW   | line=%-4d | name=%s".formatted(p.line(), p.methodName());
            case SegmentProbe p -> "SEGMENT | line=%-4d..%-4d | name=%s".formatted(p.startLine(), p.endLine(), p.methodName());
        };
    }

    private PlanFormatter() {}
}

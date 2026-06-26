package io.github.kd656.coveragex.core.report;

import io.github.kd656.coveragex.api.data.ProbeMetadata;
import io.github.kd656.coveragex.api.data.ProbeMetadataVisitor;

/**
 * Produces the single-line label string shown in the "Not covered" section of the report.
 *
 * <p>Format per probe type:
 * <ul>
 *   <li>METHOD: {@code [METHOD]  methodName(): lines S–E   hits: 0}</li>
 *   <li>BRANCH: {@code [BRANCH]  conditionText: DIRECTION not taken   line L}</li>
 *   <li>RETURN: {@code [RETURN]  line L   hits: 0}</li>
 *   <li>THROW:  {@code [THROW]   line L   hits: 0}</li>
 * </ul>
 * </p>
 *
 * <p>This class contains no branching on probe types — all dispatch is handled by the
 * visitor pattern baked into the {@link ProbeMetadata} sealed hierarchy.</p>
 */
public class ProbeMetadataLabelFormatter implements ProbeMetadataVisitor<String> {

    @Override
    public String visit(ProbeMetadata.MethodProbe p) {
        return String.format("[METHOD]  %s(): lines %d–%d   hits: 0",
                MethodNameFormatter.format(p.methodName()), p.startLine(), p.endLine());
    }

    @Override
    public String visit(ProbeMetadata.BranchProbe p) {
        return String.format("[BRANCH]  %s: %s not taken   line %d",
                p.conditionText(), p.direction(), p.line());
    }

    @Override
    public String visit(ProbeMetadata.ReturnProbe p) {
        return String.format("[RETURN]  line %d   hits: 0", p.line());
    }

    @Override
    public String visit(ProbeMetadata.ThrowProbe p) {
        return String.format("[THROW]   line %d   hits: 0", p.line());
    }

    @Override
    public String visit(ProbeMetadata.SegmentProbe p) {
        return String.format("[SEGMENT] %s(): lines %d–%d   hits: 0",
                MethodNameFormatter.format(p.methodName()), p.startLine(), p.endLine());
    }
}

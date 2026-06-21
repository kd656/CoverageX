package io.github.kd656.coveragex.api.data;

public interface ProbeMetadataVisitor<R> {
    R visit(ProbeMetadata.MethodProbe probe);
    R visit(ProbeMetadata.BranchProbe probe);
    R visit(ProbeMetadata.ReturnProbe probe);
    R visit(ProbeMetadata.ThrowProbe probe);
    R visit(ProbeMetadata.SegmentProbe probe);
}

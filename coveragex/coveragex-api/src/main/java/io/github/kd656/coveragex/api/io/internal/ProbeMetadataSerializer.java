package io.github.kd656.coveragex.api.io.internal;

import io.github.kd656.coveragex.api.data.ProbeMetadata;
import io.github.kd656.coveragex.api.data.ProbeMetadataVisitor;

import java.io.DataOutput;
import java.io.IOException;
import java.io.UncheckedIOException;

class ProbeMetadataSerializer implements ProbeMetadataVisitor<Void> {

    private final DataOutput out;

    ProbeMetadataSerializer(DataOutput out) { this.out = out; }

    @Override
    public Void visit(ProbeMetadata.MethodProbe p) {
        try {
            out.writeInt(p.startLine());
            out.writeInt(p.endLine());
        } catch (IOException e) { throw new UncheckedIOException(e); }
        return null;
    }

    @Override
    public Void visit(ProbeMetadata.BranchProbe p) {
        try {
            out.writeInt(p.line());
            out.writeUTF(p.conditionText());
            out.writeUTF(p.direction().name());
        } catch (IOException e) { throw new UncheckedIOException(e); }
        return null;
    }

    @Override
    public Void visit(ProbeMetadata.ReturnProbe p) {
        try {
            out.writeInt(p.line());
        } catch (IOException e) { throw new UncheckedIOException(e); }
        return null;
    }

    @Override
    public Void visit(ProbeMetadata.ThrowProbe p) {
        try {
            out.writeInt(p.line());
        } catch (IOException e) { throw new UncheckedIOException(e); }
        return null;
    }

    @Override
    public Void visit(ProbeMetadata.SegmentProbe p) {
        try {
            out.writeInt(p.startLine());
            out.writeInt(p.endLine());
        } catch (IOException e) { throw new UncheckedIOException(e); }
        return null;
    }
}

package io.github.kd656.coveragex.api.io.internal;

import io.github.kd656.coveragex.api.data.ProbeMetadata;
import io.github.kd656.coveragex.api.data.ProbeMetadataVisitor;

import java.io.DataOutput;
import java.io.IOException;
import java.io.UncheckedIOException;

class ProbeMetadataSerializer implements ProbeMetadataVisitor<Void> {

    private final DataOutput out;

    ProbeMetadataSerializer(DataOutput out) { this.out = out; }

    /**
     * Serialises a {@link ProbeMetadata.MethodProbe} to the output stream.
     *
     * <p>Field order:</p>
     * <ol>
     *   <li>{@code int}    — start line</li>
     *   <li>{@code int}    — end line</li>
     *   <li>{@code int}    — parameter name count</li>
     *   <li>{@code UTF}×N — parameter names</li>
     * </ol>
     *
     * @param p the probe to serialise
     * @return {@code null} (visitor convention)
     */
    @Override
    public Void visit(ProbeMetadata.MethodProbe p) {
        try {
            out.writeInt(p.startLine());
            out.writeInt(p.endLine());
            out.writeInt(p.parameterNames().size());
            for (String name : p.parameterNames()) {
                out.writeUTF(name);
            }
        } catch (IOException e) { throw new UncheckedIOException(e); }
        return null;
    }

    /**
     * Serialises a {@link ProbeMetadata.BranchProbe} to the output stream.
     *
     * <p>Field order:</p>
     * <ol>
     *   <li>{@code int}    — source line</li>
     *   <li>{@code UTF}    — condition text</li>
     *   <li>{@code UTF}    — direction name</li>
     *   <li>{@code int}    — condition id</li>
     *   <li>{@code int}    — operand kind stable code (see {@link io.github.kd656.coveragex.api.data.OperandKind#code()})</li>
     *   <li>{@code int}    — arg label count</li>
     *   <li>{@code UTF}×N — arg labels</li>
     * </ol>
     *
     * @param p the probe to serialise
     * @return {@code null} (visitor convention)
     */
    @Override
    public Void visit(ProbeMetadata.BranchProbe p) {
        try {
            out.writeInt(p.line());
            out.writeUTF(p.conditionText());
            out.writeUTF(p.direction().name());
            out.writeInt(p.conditionId());
            out.writeInt(p.kind().code());
            out.writeInt(p.argLabels().size());
            for (String label : p.argLabels()) {
                out.writeUTF(label);
            }
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

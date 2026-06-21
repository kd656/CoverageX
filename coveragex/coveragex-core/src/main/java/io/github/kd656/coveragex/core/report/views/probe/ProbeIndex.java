package io.github.kd656.coveragex.core.report.views.probe;

import io.github.kd656.coveragex.api.data.ProbeMetadata;
import io.github.kd656.coveragex.api.data.ProbeMetadata.BranchProbe;
import io.github.kd656.coveragex.api.data.ProbeMetadata.MethodProbe;
import io.github.kd656.coveragex.api.data.ProbeMetadata.ReturnProbe;
import io.github.kd656.coveragex.api.data.ProbeMetadata.SegmentProbe;
import io.github.kd656.coveragex.api.data.ProbeMetadata.ThrowProbe;
import io.github.kd656.coveragex.api.data.ProbeMetadataVisitor;

import java.util.*;

/**
 * Builds and holds two line-keyed indexes over a class's probe metadata.
 * Replaces the instanceof-based buildProbesByLine / buildLineToMethodIndex helpers.
 */
public final class ProbeIndex {

    private final Map<Integer, List<ProbeMetadata>> byLine;
    private final Map<Integer, MethodProbe> methodByLine;

    private ProbeIndex(Map<Integer, List<ProbeMetadata>> byLine,
                       Map<Integer, MethodProbe> methodByLine) {
        this.byLine = Collections.unmodifiableMap(byLine);
        this.methodByLine = Collections.unmodifiableMap(methodByLine);
    }

    public static ProbeIndex build(List<ProbeMetadata> metadata) {
        Map<Integer, List<ProbeMetadata>> byLine = new HashMap<>();
        Map<Integer, MethodProbe> methodByLine = new HashMap<>();

        ProbeMetadataVisitor<Void> indexer = new ProbeMetadataVisitor<>() {

            @Override
            public Void visit(MethodProbe p) {
                // byLine: only at startLine — used for entry detection / classification
                byLine.computeIfAbsent(p.startLine(), k -> new ArrayList<>()).add(p);
                // methodByLine: full range — used to know which method covers a given line
                for (int ln = p.startLine(); ln <= p.endLine(); ln++) {
                    methodByLine.merge(ln, p,
                        (existing, mp2) -> mp2.startLine() > existing.startLine() ? mp2 : existing);
                }
                return null;
            }

            @Override
            public Void visit(BranchProbe p) {
                byLine.computeIfAbsent(p.line(), k -> new ArrayList<>()).add(p);
                return null;
            }

            @Override
            public Void visit(ReturnProbe p) {
                byLine.computeIfAbsent(p.line(), k -> new ArrayList<>()).add(p);
                return null;
            }

            @Override
            public Void visit(ThrowProbe p) {
                byLine.computeIfAbsent(p.line(), k -> new ArrayList<>()).add(p);
                return null;
            }

            @Override
            public Void visit(SegmentProbe p) {
                // Segment covers a range; add to every line in that range
                for (int ln = p.startLine(); ln <= p.endLine(); ln++) {
                    byLine.computeIfAbsent(ln, k -> new ArrayList<>()).add(p);
                }
                return null;
            }
        };

        for (ProbeMetadata pm : metadata) {
            if (pm != null) pm.accept(indexer);
        }

        return new ProbeIndex(byLine, methodByLine);
    }

    /** All probes whose coverage applies to {@code line}. Never null. */
    public List<ProbeMetadata> probesAt(int line) {
        return byLine.getOrDefault(line, Collections.emptyList());
    }

    /** The innermost method probe that covers {@code line}, or empty. */
    public Optional<MethodProbe> methodAt(int line) {
        return Optional.ofNullable(methodByLine.get(line));
    }
}

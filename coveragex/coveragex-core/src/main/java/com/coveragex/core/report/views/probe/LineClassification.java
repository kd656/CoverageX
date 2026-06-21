package com.coveragex.core.report.views.probe;

import com.coveragex.api.data.ProbeMetadata;
import com.coveragex.api.data.ProbeMetadata.BranchProbe;
import com.coveragex.api.data.ProbeMetadata.MethodProbe;
import com.coveragex.api.data.ProbeMetadataVisitor;

import java.util.List;

/**
 * Classifies the probes at a single source line for rendering decisions.
 */
public record LineClassification(boolean isMethodEntry, boolean hasBranch, MethodProbe firstMethodProbe) {

    public static LineClassification of(List<ProbeMetadata> lineProbes) {
        boolean[] isMethod = {false};
        boolean[] isBranch = {false};
        MethodProbe[] first = {null};

        ProbeMetadataVisitor<Void> classifier = new ProbeMetadataVisitor<>() {
            @Override
            public Void visit(MethodProbe p) {
                isMethod[0] = true;
                if (first[0] == null) first[0] = p;
                return null;
            }
            @Override public Void visit(BranchProbe p) { isBranch[0] = true; return null; }
            @Override public Void visit(ProbeMetadata.ReturnProbe p)   { return null; }
            @Override public Void visit(ProbeMetadata.ThrowProbe p)    { return null; }
            @Override public Void visit(ProbeMetadata.SegmentProbe p) { return null; }
        };

        for (ProbeMetadata p : lineProbes) p.accept(classifier);

        return new LineClassification(isMethod[0], isBranch[0], first[0]);
    }
}

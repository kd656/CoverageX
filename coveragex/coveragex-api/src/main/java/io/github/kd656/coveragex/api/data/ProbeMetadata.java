package io.github.kd656.coveragex.api.data;

public sealed interface ProbeMetadata
        permits ProbeMetadata.MethodProbe,
                ProbeMetadata.BranchProbe,
                ProbeMetadata.ReturnProbe,
                ProbeMetadata.ThrowProbe,
                ProbeMetadata.SegmentProbe {

    int probeId();
    String methodName();
    int lineNumber();
    String tag();
    <R> R accept(ProbeMetadataVisitor<R> visitor);

    record MethodProbe(int probeId, String methodName, int startLine, int endLine)
            implements ProbeMetadata {
        public static final String TAG = "METHOD";
        @Override public int lineNumber() { return startLine; }
        @Override public String tag()     { return TAG; }
        @Override public <R> R accept(ProbeMetadataVisitor<R> v) { return v.visit(this); }
    }

    record BranchProbe(int probeId, String methodName, int line, String conditionText, BranchDirection direction)
            implements ProbeMetadata {
        public static final String TAG = "BRANCH";
        @Override public int lineNumber() { return line; }
        @Override public String tag()     { return TAG; }
        @Override public <R> R accept(ProbeMetadataVisitor<R> v) { return v.visit(this); }
    }

    record ReturnProbe(int probeId, String methodName, int line)
            implements ProbeMetadata {
        public static final String TAG = "RETURN";
        @Override public int lineNumber() { return line; }
        @Override public String tag()     { return TAG; }
        @Override public <R> R accept(ProbeMetadataVisitor<R> v) { return v.visit(this); }
    }

    record ThrowProbe(int probeId, String methodName, int line)
            implements ProbeMetadata {
        public static final String TAG = "THROW";
        @Override public int lineNumber() { return line; }
        @Override public String tag()     { return TAG; }
        @Override public <R> R accept(ProbeMetadataVisitor<R> v) { return v.visit(this); }
    }

    record SegmentProbe(int probeId, String methodName, int startLine, int endLine)
            implements ProbeMetadata {
        public static final String TAG = "SEGMENT";
        @Override public int lineNumber() { return startLine; }
        @Override public String tag()     { return TAG; }
        @Override public <R> R accept(ProbeMetadataVisitor<R> v) { return v.visit(this); }
    }

    enum BranchDirection {
        TRUE,
        FALSE
    }
}

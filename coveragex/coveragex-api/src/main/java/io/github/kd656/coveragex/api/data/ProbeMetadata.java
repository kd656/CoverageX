package io.github.kd656.coveragex.api.data;

import java.util.List;

/**
 * Sealed hierarchy of probe metadata types. One subtype per probe kind;
 * each subtype carries only the fields relevant to its kind.
 */
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

    /**
     * Probe metadata for a method entry point.
     *
     * @param probeId        the runtime probe id
     * @param methodName     the method's simple name
     * @param startLine      the first source line of the method declaration
     * @param endLine        the last source line of the method body
     * @param parameterNames source-level parameter names in declaration order;
     *                       empty when the method has no parameters or when no
     *                       source map was available at instrumentation time
     */
    record MethodProbe(int probeId, String methodName, int startLine, int endLine,
                       List<String> parameterNames)
            implements ProbeMetadata {

        public static final String TAG = "METHOD";

        public MethodProbe {
            parameterNames = List.copyOf(parameterNames);
        }

        @Override public int lineNumber() { return startLine; }
        @Override public String tag()     { return TAG; }
        @Override public <R> R accept(ProbeMetadataVisitor<R> v) { return v.visit(this); }
    }

    /**
     * Probe metadata for one direction of one boolean operand within a conditional.
     *
     * @param probeId        the runtime probe id
     * @param methodName     the enclosing method's simple name
     * @param line           the source line of the operand
     * @param conditionText  verbatim source text of the operand
     * @param direction      {@code TRUE} or {@code FALSE}
     * @param conditionId    1-based operand index within the parent decision;
     *                       {@code -1} when no source map is available
     * @param kind           structural classification of the operand; used to
     *                       pick column headers and select the bytecode capture strategy
     * @param argLabels      non-literal operand argument labels, in source order,
     *                       used as column headers in the per-direction test table;
     *                       empty when no source map is available or when the
     *                       operand produces no capturable arguments
     */
    record BranchProbe(int probeId,
                       String methodName,
                       int line,
                       String conditionText,
                       BranchDirection direction,
                       int conditionId,
                       OperandKind kind,
                       List<String> argLabels)
            implements ProbeMetadata {

        public static final String TAG = "BRANCH";

        public BranchProbe {
            argLabels = List.copyOf(argLabels);
        }

        /**
         * Returns the number of capturable operand values for this probe direction.
         * Equivalent to {@code argLabels().size()}.
         *
         * @return the operand value count
         */
        public int operandValueCount() {
            return argLabels.size();
        }

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

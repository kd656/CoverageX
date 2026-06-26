package io.github.kd656.coveragex.core.analysis.source.impl;

import com.github.javaparser.ast.expr.Expression;

/**
 * Derives the column-label schema and instrument-time capture metadata for one leaf
 * boolean operand. Dispatches over the AST node type via {@link OperandArgVisitor};
 * literal arguments and non-simple receivers are filtered out at the visit-method level.
 *
 * <p>The returned {@link OperandArgVisitor.LeafResult} bundles:</p>
 * <ul>
 *   <li>column labels — used as column headers in the HTML report popover;</li>
 *   <li>method-call capture mask — bitmask encoding which stack positions to spill
 *       before the call site (bit 0 = receiver, bits 1..N = args);</li>
 *   <li>method-call name and arg count — name+arity matching guard used in
 *       {@code visitMethodInsn} to skip nested calls that share the same
 *       bytecode stream.</li>
 * </ul>
 *
 * <p>This class is a stateless façade; all dispatch logic lives in
 * {@link OperandArgVisitor}. The singleton visitor is thread-safe because it carries
 * no mutable state.</p>
 */
final class OperandArgDeriver {

    private static final OperandArgVisitor VISITOR = new OperandArgVisitor();

    private OperandArgDeriver() {
    }

    /**
     * Returns the full analyse-time result for the supplied leaf operand expression.
     *
     * <p>Examples:</p>
     * <ul>
     *   <li>{@code name.startsWith(".")} → labels {@code ["name"]}, mask {@code 1},
     *       name {@code "startsWith"}, arity {@code 1}</li>
     *   <li>{@code a == b} → labels {@code ["a","b"]}, mask {@code 0}, name
     *       {@code null}, arity {@code 0}</li>
     *   <li>{@code Math.max(x, y) > 0} → labels {@code []}, mask {@code 0}, name
     *       {@code null}, arity {@code 0} (type reference receiver filtered)</li>
     * </ul>
     *
     * @param leaf the leaf {@link Expression} taken from the decision's flattened operand list
     * @return the derive result; never {@code null}
     */
    static OperandArgVisitor.LeafResult derive(Expression leaf) {
        return leaf.accept(VISITOR, null);
    }
}

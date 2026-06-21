package com.coveragex.core.analysis.source.model;

/**
 * Represents a single leaf operand in a boolean condition expression.
 *
 * <p>For a condition like {@code a != null && b.isEmpty()}, there are two
 * operands: one for {@code a != null} and one for {@code b.isEmpty()}.</p>
 *
 * <p>The {@link #conditionText} field holds the verbatim source text extracted
 * during the analyze phase via {@code Expression.toString()} from the JavaParser
 * AST. It is {@code null} when the analyze phase was unable to capture the text
 * (e.g., an old map file or a node without position information).</p>
 *
 * @param conditionId    1-based leaf index within the decision's operands list
 * @param range          precise source coordinates of this operand expression
 * @param conditionText  verbatim source text of this operand (nullable — absent
 *                       for old map files; fall back to opcode placeholder)
 * @param jumpMeansTrue  {@code true} if the bytecode conditional jump for this
 *                       operand fires when the operand evaluates to {@code true};
 *                       {@code false} if it fires when the operand is {@code false}.
 *                       Derived from the boolean context (OR vs AND position) during
 *                       source analysis. Operands in an {@code ||} chain have
 *                       {@code jumpMeansTrue = true}; operands in an {@code &&} chain
 *                       have {@code jumpMeansTrue = false}.
 */
public record OperandModel(int conditionId, Range range, String conditionText, boolean jumpMeansTrue) {

    /**
     * Convenience constructor for backward-compatibility and testing when
     * conditionText is not yet available.
     *
     * @param conditionId 1-based leaf index
     * @param range       source coordinate range
     */
    public OperandModel(int conditionId, Range range) {
        this(conditionId, range, null, false);
    }

    /**
     * Convenience constructor for backward-compatibility and testing when
     * jumpMeansTrue is not yet available.
     *
     * @param conditionId   1-based leaf index
     * @param range         source coordinate range
     * @param conditionText verbatim source text
     */
    public OperandModel(int conditionId, Range range, String conditionText) {
        this(conditionId, range, conditionText, false);
    }
}

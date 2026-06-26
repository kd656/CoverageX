package io.github.kd656.coveragex.core.analysis.source.model;

import io.github.kd656.coveragex.api.data.OperandKind;

import java.util.List;

/**
 * Represents a single leaf operand in a boolean condition expression.
 *
 * <p>For a condition like {@code a != null && b.isEmpty()}, there are two
 * operands: one for {@code a != null} and one for {@code b.isEmpty()}.</p>
 *
 * @param conditionId             1-based leaf index within the decision's operands list
 * @param range                   precise source coordinates of this operand expression
 * @param conditionText           verbatim source text of this operand, captured during
 *                                analysis via {@code Expression.toString()} on the
 *                                JavaParser AST
 * @param jumpMeansTrue           {@code true} if the bytecode conditional jump for this
 *                                operand fires when the operand evaluates to {@code true};
 *                                {@code false} if it fires when the operand is {@code false}.
 *                                Derived from the boolean context (OR vs AND position) during
 *                                source analysis. Operands in an {@code ||} chain have
 *                                {@code jumpMeansTrue = true}; operands in an {@code &&} chain
 *                                have {@code jumpMeansTrue = false}.
 * @param kind                    structural classification of the operand, used to pick a
 *                                column-header rendering strategy and a bytecode capture
 *                                strategy
 * @param argLabels               non-literal operand argument labels, in source order, used
 *                                as column headers in the per-direction test table; empty when
 *                                the operand produces no capturable arguments
 * @param binaryCaptureMask       bitmask for {@link io.github.kd656.coveragex.api.data.OperandKind#BINARY_COMPARE}
 *                                operands only: bit 0 set means the left-hand operand is
 *                                capturable (not a literal); bit 1 set means the right-hand
 *                                operand is capturable. Zero for non-binary operands and when
 *                                either operand side is a long or floating-point literal
 *                                (category-2 comparison result cannot be captured at the
 *                                {@code IF*} site). Instrument-time only; not persisted.
 * @param methodCallCaptureMask   bitmask for {@link io.github.kd656.coveragex.api.data.OperandKind#METHOD_CALL}
 *                                operands: bit 0 = capture receiver (instance calls; ignored
 *                                for static); bit N (N ≥ 1) = capture AST argument N-1.
 *                                Instrument-time only; not persisted.
 * @param methodCallName          simple AST method name for the outermost
 *                                {@link io.github.kd656.coveragex.api.data.OperandKind#METHOD_CALL}
 *                                operand, used as a name-matching guard in
 *                                {@code visitMethodInsn} to reject nested calls that share
 *                                the same bytecode stream. {@code null} for non-METHOD_CALL
 *                                operands and unmatched cases. Instrument-time only.
 * @param methodCallArgCount      AST argument count for the outermost METHOD_CALL operand
 *                                (taken from {@code expr.getArguments().size()} at analyse
 *                                time). Together with {@link #methodCallName} this forms the
 *                                arity-matching guard that rejects overloads with a different
 *                                shape. {@code 0} for non-METHOD_CALL operands.
 *                                Instrument-time only; not persisted.
 */
public record OperandModel(int conditionId,
                           Range range,
                           String conditionText,
                           boolean jumpMeansTrue,
                           OperandKind kind,
                           List<String> argLabels,
                           int binaryCaptureMask,
                           int methodCallCaptureMask,
                           String methodCallName,
                           int methodCallArgCount) {

    public OperandModel {
        argLabels = List.copyOf(argLabels);
    }
}

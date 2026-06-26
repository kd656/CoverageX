package io.github.kd656.coveragex.core.analysis.source.impl;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.visitor.GenericVisitorWithDefaults;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-node-type extraction of operand column labels plus the instrument-time
 * capture mask and method-call matching key. Built on JavaParser's generic visitor.
 * Each override is responsible for one operand kind; everything unhandled falls
 * through to {@link #defaultAction} and returns a zero-mask result with no labels.
 *
 * <p><b>Receivers we keep.</b> A receiver is the expression before the {@code .} in a
 * method call. We capture it only when it is a "simple reference" — a {@link NameExpr}
 * ({@code name}), a {@link FieldAccessExpr} ({@code obj.field}, {@code this.name}) or a
 * {@link ThisExpr}. These map cleanly to a column header that the reader can locate in
 * the source.
 *
 * <p><b>Receivers we drop.</b> Anything else — most commonly a {@link MethodCallExpr}
 * used as a scope, e.g. {@code getName().startsWith(".")} — would produce a column
 * header like "{@code getName()}", which adds no value over the condition text itself
 * and whose captured value can vary per invocation. Array accesses, casts, and
 * {@code new} expressions used as receivers are dropped for the same reason.
 *
 * <p><b>Literals.</b> Literal arguments and operands are dropped because they already
 * appear in the operand's condition text; a column of identical constants is noise.
 *
 * <p>All visitor overrides return a {@link LeafResult} that bundles the column labels,
 * the method-call capture mask, the method name, and the AST argument count. Only
 * the {@code visit(MethodCallExpr)} override sets non-zero mask / non-null name values;
 * all other overrides set mask to {@code 0}, name to {@code null}, and arity to {@code 0}.
 */
final class OperandArgVisitor extends GenericVisitorWithDefaults<OperandArgVisitor.LeafResult, Void> {

    /**
     * Bundles the analyse-time outputs from one visitor invocation.
     *
     * @param labels               column labels in source order; immutable
     * @param methodCallCaptureMask  bitmask: bit 0 = capture receiver (ignored for static
     *                               calls); bit N = capture AST argument N-1.
     *                               {@code 0} for non-METHOD_CALL operands.
     * @param methodCallName         simple AST method name; {@code null} for non-METHOD_CALL
     * @param methodCallArgCount     AST argument count; {@code 0} for non-METHOD_CALL
     */
    public record LeafResult(List<String> labels,
                             int methodCallCaptureMask,
                             String methodCallName,
                             int methodCallArgCount) {

        public LeafResult {
            labels = List.copyOf(labels);
        }

        /**
         * Convenience factory for the truly-empty case where there are no labels
         * and no capture (e.g. an unsupported AST node type). Do not use when
         * the labels list merely happens to be empty but the operand has a known
         * structure (e.g. a literal-only method call) — in that case return a
         * {@code LeafResult} with the correct name and arity so the instrument-time
         * matching guard can still fire.
         *
         * @return a result with empty labels, zero mask, null name, and zero arity
         */
        public static LeafResult empty() {
            return new LeafResult(List.of(), 0, null, 0);
        }
    }

    /**
     * Extracts column labels and the capture mask for a method-call operand.
     *
     * <p>Labels come from two sources: the receiver (when it is a simple reference and
     * not a type name), and any non-literal arguments. Example: {@code name.startsWith(".")}
     * produces {@code labels = ["name"]}, {@code mask = 0b01} (receiver bit only), name
     * {@code "startsWith"}, arity {@code 1}.</p>
     *
     * @param expr    the method-call expression
     * @param ignored unused visitor argument
     * @return the {@link LeafResult} for this call
     */
    @Override
    public LeafResult visit(MethodCallExpr expr, Void ignored) {
        List<String> labels = new ArrayList<>();
        int mask = 0;

        // Bit 0 — receiver, if it is a simple non-type reference.
        if (expr.getScope().isPresent()
                && isSimpleReference(expr.getScope().get())
                && !isTypeReference(expr.getScope().get())) {
            labels.add(expr.getScope().get().toString());
            mask |= 1;
        }

        // Bits 1..N — AST args in source order; literals filtered.
        NodeList<Expression> args = expr.getArguments();
        for (int i = 0; i < args.size(); i++) {
            Expression arg = args.get(i);
            if (!arg.isLiteralExpr()) {
                labels.add(arg.toString());
                mask |= (1 << (i + 1));
            }
        }
        return new LeafResult(labels, mask, expr.getNameAsString(), args.size());
    }

    /**
     * Extracts column labels for a binary-comparison operand.
     *
     * <p>Both the left-hand and right-hand sub-expressions are captured unless they
     * are literals. Example: {@code a == b} produces {@code ["a", "b"]};
     * {@code x > 5} produces {@code ["x"]} (the literal {@code 5} is suppressed).
     * Mask and matching key are {@code 0}/{@code null}/{@code 0} — the binary-compare
     * path does not use the method-call capture mechanism.</p>
     *
     * @param expr    the binary expression
     * @param ignored unused visitor argument
     * @return a {@link LeafResult} with the derived labels and zero mask
     */
    @Override
    public LeafResult visit(BinaryExpr expr, Void ignored) {
        List<String> labels = new ArrayList<>(2);
        addIfCapturable(labels, expr.getLeft());
        addIfCapturable(labels, expr.getRight());
        return new LeafResult(labels, 0, null, 0);
    }

    /**
     * Extracts the column label for a unary-negation operand.
     *
     * <p>The inner expression is captured unless it is a literal. Example:
     * {@code !flag} produces {@code ["flag"]}.</p>
     *
     * @param expr    the unary expression
     * @param ignored unused visitor argument
     * @return a {@link LeafResult} with a single-element labels list, or empty labels
     */
    @Override
    public LeafResult visit(UnaryExpr expr, Void ignored) {
        Expression inner = expr.getExpression();
        List<String> labels = inner.isLiteralExpr() ? List.of() : List.of(inner.toString());
        return new LeafResult(labels, 0, null, 0);
    }

    /**
     * Extracts the column label for a bare boolean reference (local or field read).
     *
     * @param expr    the name expression
     * @param ignored unused visitor argument
     * @return a {@link LeafResult} containing the variable name
     */
    @Override
    public LeafResult visit(NameExpr expr, Void ignored) {
        return new LeafResult(List.of(expr.getNameAsString()), 0, null, 0);
    }

    /**
     * Extracts the column label for a field access expression such as
     * {@code this.active} or {@code obj.flag}.
     *
     * @param expr    the field access expression
     * @param ignored unused visitor argument
     * @return a {@link LeafResult} containing the full field access text
     */
    @Override
    public LeafResult visit(FieldAccessExpr expr, Void ignored) {
        return new LeafResult(List.of(expr.toString()), 0, null, 0);
    }

    /**
     * Default fallback for any node type not explicitly handled.
     *
     * @param node    any AST node
     * @param ignored unused visitor argument
     * @return an empty {@link LeafResult}
     */
    @Override
    public LeafResult defaultAction(Node node, Void ignored) {
        return LeafResult.empty();
    }

    /**
     * Default fallback for {@link NodeList} which is not a {@link Node}.
     *
     * @param list    a node list
     * @param ignored unused visitor argument
     * @return an empty {@link LeafResult}
     */
    @Override
    public LeafResult defaultAction(NodeList list, Void ignored) {
        return LeafResult.empty();
    }

    private void addIfCapturable(List<String> sink, Expression expr) {
        if (!expr.isLiteralExpr() && !expr.isNullLiteralExpr()) {
            sink.add(expr.toString());
        }
    }

    private boolean isSimpleReference(Expression expr) {
        return expr instanceof NameExpr
                || expr instanceof FieldAccessExpr
                || expr instanceof ThisExpr;
    }

    /**
     * Heuristic for "is this scope a type name rather than a value"?
     *
     * <p>{@code Math.max(a, 5)} parses as a {@link MethodCallExpr} with scope
     * {@link NameExpr}{@code ("Math")} — but {@code Math} is a class, not a captured
     * value, so it would produce a useless column header.</p>
     *
     * <p>Identifiers starting with an upper-case letter are taken to be type references.
     * Edge cases (a local variable named {@code Foo}, or a static-import call with no
     * explicit scope) are accepted as a known false-positive rate; symbol resolution via
     * JavaParser's solver would be exact but adds a hard dependency on
     * {@code javaparser-symbol-solver-core}.</p>
     *
     * @param scope the scope expression to test
     * @return {@code true} if the scope looks like a type reference
     */
    private boolean isTypeReference(Expression scope) {
        if (!(scope instanceof NameExpr name)) {
            return false;
        }
        String id = name.getNameAsString();
        return !id.isEmpty() && Character.isUpperCase(id.charAt(0));
    }
}

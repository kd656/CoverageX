package io.github.kd656.coveragex.core.analysis.source.impl;

import io.github.kd656.coveragex.core.analysis.source.DescriptorResolver;
import io.github.kd656.coveragex.core.analysis.source.SourceAnalyzer;
import io.github.kd656.coveragex.core.analysis.source.model.*;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class SourceCodeAnalyzer implements SourceAnalyzer {

    private final JavaParser parser;
    private final SemanticIndex index;
    private final DescriptorResolver descriptorResolver = DescriptorResolver.defaultResolver();
    private static final Logger LOG = LoggerFactory.getLogger(SourceCodeAnalyzer.class);

    public SourceCodeAnalyzer(JavaParser parser, SemanticIndex index) {
        this.parser = Objects.requireNonNull(parser);
        this.index = Objects.requireNonNull(index);
    }

    /**
     * Recursively scans a source root like src/main/java
     */
    public void scan(Path root) throws IOException {
        if (root == null || !Files.exists(root)) {
            LOG.debug("Source root does not exist: {}", root);
            return;
        }

        try (var stream = Files.walk(root)) {
            stream.filter(p -> p.toString().endsWith(".java")).forEach(p -> parse(root, p));
        }
    }

    private void parse(Path root, Path file) {
        try {
            LOG.info("Parsing a file: " + file.toString());
            ParseResult<CompilationUnit> parseResult = parser.parse(file);
            if (parseResult.getResult().isEmpty()) {
                LOG.warn("Failed to parse: " + file);
                return;
            }

            CompilationUnit compilationUnit = parseResult.getResult().get();

            // for reporting, store path relative to root
            String relSourcePath = root.relativize(file).toString().replace('\\', '/');

            new DecisionVisitor(relSourcePath, descriptorResolver, compilationUnit, index).visit(compilationUnit, null);

        } catch (Exception e) {
            LOG.warn("Error parsing " + file + ": " + e.getMessage());
        }
    }

    private static final class DecisionVisitor extends VoidVisitorAdapter<Void> {

        /**
         * Per-visitor counter — a fresh {@code DecisionVisitor} is constructed for every
         * source file (see {@link SourceCodeAnalyzer#parse(Path, Path)}), so decision IDs
         * restart at 1 for each file. Previously this counter was {@code static}, which
         * leaked state across analyzer invocations in the same JVM and produced
         * order-dependent IDs in test suites.
         */
        private final AtomicInteger nextDecisionId = new AtomicInteger(1);

        private final String sourceFile;
        private final SemanticIndex index;
        private final CompilationUnit compilationUnit;
        private final DescriptorResolver descriptorResolver;

        private static final Logger LOG = LoggerFactory.getLogger(DecisionVisitor.class);

        DecisionVisitor(String sourceFile, DescriptorResolver descriptorResolver,
                        CompilationUnit compilationUnit, SemanticIndex index) {

            this.sourceFile = sourceFile;
            this.index = index;
            this.descriptorResolver = descriptorResolver;
            this.compilationUnit = compilationUnit;
        }

        @Override
        public void visit(IfStmt n, Void arg) {
            super.visit(n, arg);
            recordDecision("IF", n.getCondition(), n);
        }

        @Override
        public void visit(WhileStmt n, Void arg) {
            super.visit(n, arg);
            recordDecision("WHILE", n.getCondition(), n);
        }

        @Override
        public void visit(DoStmt n, Void arg) {
            super.visit(n, arg);
            recordDecision("DO", n.getCondition(), n);
        }

        @Override
        public void visit(ForStmt n, Void arg) {
            super.visit(n, arg);
            n.getCompare().ifPresent(c -> recordDecision("FOR", c, n));
        }

        @Override
        public void visit(ConditionalExpr n, Void arg) {
            super.visit(n, arg);
            recordDecision("TERNARY", n.getCondition(), n);
        }

        @Override
        public void visit(AssertStmt n, Void arg) {
            super.visit(n, arg);
            recordDecision("ASSERT", n.getCheck(), n);
        }

        @Override
        public void visit(ReturnStmt n, Void arg) {
            super.visit(n, arg);
            n.getExpression().ifPresent(expr -> {
                if (isBooleanExpression(unwrap(expr))) {
                    recordDecision("RETURN", expr, n);
                }
            });
        }

        @Override
        public void visit(MethodDeclaration n, Void arg) {
            super.visit(n, arg);
            // Register every method eagerly so SourceAwareProbeInjector can resolve
            // the declaration line even for methods that have no branch decisions.
            String className = findClassName(n);
            if (className != null) {
                ClassModel classModel = index.getOrCreateClass(className, sourceFile);
                String descriptor = descriptorResolver.resolveMethodDescriptor(compilationUnit, n);
                classModel.getOrCreate(new MethodReference(n.getNameAsString(), descriptor),
                        () -> buildFromMethod(className, n));
            }
        }

        @Override
        public void visit(ConstructorDeclaration n, Void arg) {
            super.visit(n, arg);
            String className = findClassName(n);
            if (className != null) {
                ClassModel classModel = index.getOrCreateClass(className, sourceFile);
                String descriptor = descriptorResolver.resolveConstructorDescriptor(compilationUnit, n);
                classModel.getOrCreate(new MethodReference("<init>", descriptor),
                        () -> buildFromConstructor(className, n));
            }
        }

        private void recordDecision(String kind, Expression condition, Node decisionNode) {
            // must have positions (if source lacks them, skip)
            Range decisionRange = toRange(decisionNode);
            Range conditionRange = toRange(condition);

            if (decisionRange.beginLine() < 0 || conditionRange.beginLine() < 0) {
                LOG.info("Skipping the decision.");
                return;
            }

            var className = findClassName(decisionNode);
            MethodModel methodModel = constructMethodModel(className, decisionNode);
            if (className == null || methodModel == null) {
                // can happen in initializers; you can extend later
                LOG.info("Skipping decision outside method/class: {} @ {}", sourceFile, decisionRange.beginLine());
                return;
            }

            int decisionId = nextDecisionId.getAndIncrement();

            // leaf operands for condition coverage / MC/DC
            List<Expression> leaves = new ArrayList<>();
            List<Boolean> jumpMeansTrueList = new ArrayList<>();
            // Initial context is false: the final operand in any chain is always
            // evaluated with a negative opcode (e.g. IFEQ else_label), so jumpMeansTrue=false.
            flattenOperands(condition, leaves, jumpMeansTrueList, false);

            List<OperandModel> operands = new ArrayList<>(leaves.size());
            for (int i = 0; i < leaves.size(); i++) {
                Expression leaf = leaves.get(i);
                // Capture verbatim source text from the live AST node so the agent
                // can populate BranchProbe.conditionText without touching source files
                // at instrumentation time.
                String conditionText = leaf.toString();
                operands.add(new OperandModel(i + 1, toRange(leaf), conditionText, jumpMeansTrueList.get(i)));
            }

            // RPN encoding for MC/DC structure
            List<Object> rpn = new ArrayList<>();
            toRpn(condition, leaves, rpn);

            DecisionModel model = new DecisionModel(decisionId, kind, decisionRange, conditionRange, operands, rpn);
            methodModel.addDecision(model);
        }

        // ===== Expression processing =====

        /**
         * Split only on {@code &&} and {@code ||}. Everything else is a leaf operand.
         *
         * <p>Also computes {@code jumpMeansTrue} for each leaf — whether the bytecode
         * conditional jump for that operand fires when the operand is {@code true} or
         * {@code false}. This is determined by the operand's position in the boolean tree:</p>
         * <ul>
         *   <li>Non-last operands in {@code ||}: javac short-circuits to the then-body
         *       when the operand is TRUE, so {@code jumpMeansTrue = true}.</li>
         *   <li>Non-last operands in {@code &&}: javac short-circuits to the else-body
         *       when the operand is FALSE, so {@code jumpMeansTrue = false}.</li>
         *   <li>The rightmost (last) leaf inherits its parent's context.</li>
         * </ul>
         *
         * @param expr           the expression subtree to flatten
         * @param out            accumulates the leaf expressions in source order
         * @param jumpMeansTrue  parallel list accumulating the polarity for each leaf
         * @param context        the {@code jumpMeansTrue} value the current node would
         *                       inherit if it turns out to be a leaf
         */
        private void flattenOperands(Expression expr, List<Expression> out,
                                     List<Boolean> jumpMeansTrue, boolean context) {
            expr = unwrap(expr);

            if (expr.isBinaryExpr()) {
                BinaryExpr b = expr.asBinaryExpr();
                if (b.getOperator() == BinaryExpr.Operator.AND || b.getOperator() == BinaryExpr.Operator.OR) {
                    // OR left child: javac jumps to then-body when operand is TRUE
                    // AND left child: javac jumps to else-body when operand is FALSE
                    boolean leftContext = b.getOperator() == BinaryExpr.Operator.OR;
                    flattenOperands(b.getLeft(), out, jumpMeansTrue, leftContext);
                    // Right child inherits the parent's context unchanged
                    flattenOperands(b.getRight(), out, jumpMeansTrue, context);
                    return;
                }
            }
            out.add(expr);
            jumpMeansTrue.add(context);
        }

        /**
         * Convert expression to Reverse Polish notation over leaf indices.
         */
        private void toRpn(Expression expr, List<Expression> leaves, List<Object> out) {
            expr = unwrap(expr);

            if (expr.isBinaryExpr()) {
                BinaryExpr b = expr.asBinaryExpr();
                if (b.getOperator() == BinaryExpr.Operator.AND || b.getOperator() == BinaryExpr.Operator.OR) {
                    toRpn(b.getLeft(), leaves, out);
                    toRpn(b.getRight(), leaves, out);
                    out.add(b.getOperator() == BinaryExpr.Operator.AND ? "AND" : "OR");
                    return;
                }
            }

            int idx = indexOfSameNode(expr, leaves);
            out.add(idx); // 0-based leaf index
        }

        private int indexOfSameNode(Expression expr, List<Expression> leaves) {
            // Prefer identity; fallback to equals if needed
            for (int i = 0; i < leaves.size(); i++) {
                if (leaves.get(i) == expr) return i;
            }
            int eq = leaves.indexOf(expr);

            return Math.max(eq, 0);
        }

        /**
         * Returns true if the expression would cause javac to emit a conditional jump
         * instruction (IFEQ, IFNULL, IF_ICMPLT, etc.) inside a return statement.
         * Method calls that return boolean are excluded because javac emits IRETURN
         * directly for them without an intermediate branch.
         */
        private boolean isBooleanExpression(Expression expr) {
            if (expr.isBinaryExpr()) {
                BinaryExpr.Operator op = expr.asBinaryExpr().getOperator();
                return op == BinaryExpr.Operator.EQUALS
                        || op == BinaryExpr.Operator.NOT_EQUALS
                        || op == BinaryExpr.Operator.LESS
                        || op == BinaryExpr.Operator.GREATER
                        || op == BinaryExpr.Operator.LESS_EQUALS
                        || op == BinaryExpr.Operator.GREATER_EQUALS
                        || op == BinaryExpr.Operator.AND
                        || op == BinaryExpr.Operator.OR;
            }
            if (expr.isUnaryExpr()) {
                return expr.asUnaryExpr().getOperator() == UnaryExpr.Operator.LOGICAL_COMPLEMENT;
            }
            return expr.isInstanceOfExpr();
        }

        private Expression unwrap(Expression expr) {
            while (expr != null && expr.isEnclosedExpr()) {
                expr = expr.asEnclosedExpr().getInner();
            }
            return expr;
        }

        private String findClassName(Node n) {
            return n.findAncestor(ClassOrInterfaceDeclaration.class)
                    .map(ClassOrInterfaceDeclaration::getFullyQualifiedName)
                    .flatMap(x -> x)
                    .orElse(null);
        }

        private MethodModel constructMethodModel(String className, Node node) {

            ClassModel classModel = index.getOrCreateClass(className, sourceFile);

            return node.findAncestor(MethodDeclaration.class)
                            .map(methodDecl -> {
                                String methodName = methodDecl.getNameAsString();
                                String descriptor = descriptorResolver.resolveMethodDescriptor(compilationUnit, methodDecl);

                                MethodReference methodRef = new MethodReference(methodName, descriptor);

                                // cache lookup / create once
                                return classModel.getOrCreate(methodRef, () -> buildFromMethod(className, methodDecl));
                            })
                            .or(() -> node.findAncestor(ConstructorDeclaration.class)
                                    .map(constructorDecl -> {
                                        String methodName = "<init>";
                                        String descriptor = descriptorResolver.resolveConstructorDescriptor(compilationUnit, constructorDecl);

                                        MethodReference methodRef = new MethodReference(methodName, descriptor);

                                        return classModel.getOrCreate(methodRef, () -> buildFromConstructor(className, constructorDecl));
                                    })
                            )
                            .orElse(null);
        }

        private MethodModel buildFromMethod(String className, MethodDeclaration methodDecl) {
            String methodName = methodDecl.getNameAsString();
            String descriptor = descriptorResolver.resolveMethodDescriptor(compilationUnit, methodDecl);

            int parameterCount = methodDecl.getParameters().size();
            List<String> parameterTypes = extractParameterTypes(methodDecl.getParameters());

            String returnType = methodDecl.getTypeAsString();

            boolean isStatic = methodDecl.isStatic();
            boolean isPublic = methodDecl.isPublic();
            boolean isPrivate = methodDecl.isPrivate();
            boolean isProtected = methodDecl.isProtected();
            boolean isAbstract = methodDecl.isAbstract();

            int startLine = methodDecl.getType().getBegin().map(p -> p.line)
                    .orElseGet(() -> methodDecl.getBegin().map(p -> p.line).orElse(-1));
            int endLine   = methodDecl.getEnd().map(p -> p.line).orElse(-1);

            return MethodModel.createMethod(
                    className,
                    methodName,
                    descriptor,
                    parameterCount,
                    parameterTypes,
                    returnType,
                    isStatic,
                    isPublic,
                    isPrivate,
                    isProtected,
                    isAbstract,
                    startLine,
                    endLine
            );
        }

        private MethodModel buildFromConstructor(String className, ConstructorDeclaration constructorDecl) {
            String descriptor = descriptorResolver.resolveConstructorDescriptor(compilationUnit, constructorDecl);

            int parameterCount = constructorDecl.getParameters().size();
            List<String> parameterTypes = extractParameterTypes(constructorDecl.getParameters());

            boolean isPublic = constructorDecl.isPublic();
            boolean isPrivate = constructorDecl.isPrivate();
            boolean isProtected = constructorDecl.isProtected();

            int startLine = constructorDecl.getName().getBegin().map(p -> p.line)
                    .orElseGet(() -> constructorDecl.getBegin().map(p -> p.line).orElse(-1));
            int endLine   = constructorDecl.getEnd().map(p -> p.line).orElse(-1);

            return MethodModel.createConstructor(
                    className,
                    descriptor,
                    parameterCount,
                    parameterTypes,
                    isPublic,
                    isPrivate,
                    isProtected,
                    startLine,
                    endLine
            );
        }

        private List<String> extractParameterTypes(NodeList<Parameter> parameters) {
            return parameters.stream()
                    .map(p -> p.getType().asString())
                    .toList();
        }

        // ===== Range =====

        private Range toRange(Node n) {
            var b = n.getBegin();
            var e = n.getEnd();
            if (b.isEmpty() || e.isEmpty()) return new Range(-1, -1, -1, -1);
            return new Range(b.get().line, b.get().column, e.get().line, e.get().column);
        }
    }
}

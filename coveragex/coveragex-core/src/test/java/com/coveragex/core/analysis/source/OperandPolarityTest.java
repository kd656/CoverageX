package com.coveragex.core.analysis.source;

import com.coveragex.core.analysis.source.impl.SourceCodeAnalyzer;
import com.coveragex.core.analysis.source.model.ClassModel;
import com.coveragex.core.analysis.source.model.DecisionModel;
import com.coveragex.core.analysis.source.model.MethodModel;
import com.coveragex.core.analysis.source.model.OperandModel;
import com.coveragex.core.analysis.source.model.SemanticIndex;
import com.github.javaparser.JavaParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the per-operand {@link OperandModel#jumpMeansTrue()} polarity
 * computed by {@link SourceCodeAnalyzer}.
 *
 * <p>This is the middle layer of the IFNULL-polarity test pyramid. It validates that
 * the source-analysis phase correctly determines, for each leaf operand of a boolean
 * expression, whether the bytecode conditional jump emitted by javac fires when that
 * operand is {@code true} or {@code false}.</p>
 *
 * <h2>The {@code jumpMeansTrue} contract</h2>
 * <p>javac compiles short-circuit boolean expressions using two distinct patterns:</p>
 * <ul>
 *   <li>In a {@code ||} chain: the jump for each non-last operand targets the
 *       <em>then</em>-body when the operand is TRUE (e.g. {@code IFNULL then_label}
 *       for {@code x == null ||}). → {@code jumpMeansTrue = true}</li>
 *   <li>In a {@code &&} chain: the jump for each non-last operand targets the
 *       <em>else</em>-body when the operand is FALSE (e.g. {@code IFNULL else_label}
 *       for {@code x != null &&}). → {@code jumpMeansTrue = false}</li>
 *   <li>The rightmost leaf in any sub-expression always inherits its parent's
 *       context; at the top level that context is {@code false} because javac
 *       uses a negative opcode (e.g. {@code IFEQ else_label}) for the final check.</li>
 * </ul>
 *
 * <h2>Test organisation</h2>
 * <p>Each test writes a minimal Java source file to a JUnit {@code @TempDir},
 * runs {@link SourceCodeAnalyzer#scan}, and asserts the list of
 * {@link OperandModel#jumpMeansTrue()} values in left-to-right source order.</p>
 *
 * <p>The tests are grouped as:</p>
 * <ol>
 *   <li>Simple (one or two operands) — establishes the base cases.</li>
 *   <li>Three-operand chains — validates associativity handling.</li>
 *   <li>Mixed {@code ||}/{@code &&} trees — validates that context propagates
 *       correctly through nested sub-expressions.</li>
 *   <li>Null-check patterns — the exact scenarios from the IFNULL polarity bug.</li>
 * </ol>
 */
class OperandPolarityTest {

    /** Fresh temp directory per test — each test writes its own {@code Fixture.java}. */
    @TempDir
    Path srcRoot;

    private SemanticIndex index;
    private SourceCodeAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        index = new SemanticIndex();
        analyzer = new SourceCodeAnalyzer(new JavaParser(), index);
    }

    // =========================================================================
    // Source-file templates
    // =========================================================================

    /**
     * Template for conditions that operate on boolean variables {@code a}, {@code b},
     * {@code c}. Produces a syntactically valid compilation unit that JavaParser can
     * parse and analyse without requiring a Java compiler.
     */
    private static final String BOOLEAN_TEMPLATE = """
            class Fixture {
                boolean check(boolean a, boolean b, boolean c) {
                    if (%s) return true;
                    return false;
                }
            }
            """;

    /**
     * Template for conditions that operate on a {@code String s} parameter,
     * used for null-check and blank-check patterns.
     */
    private static final String STRING_TEMPLATE = """
            class Fixture {
                String check(String s) {
                    if (%s) return "yes";
                    return "no";
                }
            }
            """;

    /**
     * Template for a method that directly returns a boolean expression operating on
     * an {@code Object obj} parameter. This is the pattern in the bug report:
     * {@code return simpleObject != null;}.
     */
    private static final String RETURN_TEMPLATE = """
            class Fixture {
                boolean check(Object obj) {
                    return %s;
                }
            }
            """;

    // =========================================================================
    // Shared helper
    // =========================================================================

    /**
     * Writes a source file for the given template + condition, scans it with
     * {@link SourceCodeAnalyzer}, and returns the {@link OperandModel#jumpMeansTrue()}
     * value for each leaf operand in left-to-right order.
     *
     * <p>Assertions on the index structure are included so that a misconfigured
     * template fails with a clear message rather than a NullPointerException.</p>
     *
     * @param template  one of {@link #BOOLEAN_TEMPLATE} or {@link #STRING_TEMPLATE}
     * @param condition the condition expression to substitute into the template
     * @return {@code jumpMeansTrue} values in source order, one entry per leaf operand
     */
    private List<Boolean> polaritiesFor(String template, String condition) throws IOException {
        Path source = srcRoot.resolve("Fixture.java");
        Files.writeString(source, template.formatted(condition));

        analyzer.scan(srcRoot);

        ClassModel classModel = index.getClasses().get("Fixture");
        assertThat(classModel)
                .as("Fixture class not found in semantic index after scanning %s", source)
                .isNotNull();

        MethodModel method = classModel.getMethods().values().stream()
                .filter(m -> m.getName().equals("check"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("check() method not found in Fixture"));

        List<DecisionModel> decisions = method.getDecisionsList();
        assertThat(decisions)
                .as("Expected exactly one if-statement decision in check()")
                .hasSize(1);

        return decisions.get(0).operands().stream()
                .map(OperandModel::jumpMeansTrue)
                .toList();
    }

    /** Convenience — uses the boolean-variable template. */
    private List<Boolean> boolPolarities(String condition) throws IOException {
        return polaritiesFor(BOOLEAN_TEMPLATE, condition);
    }

    /** Convenience — uses the String-parameter template. */
    private List<Boolean> strPolarities(String condition) throws IOException {
        return polaritiesFor(STRING_TEMPLATE, condition);
    }

    /**
     * Variant of {@link #polaritiesFor} for return-expression templates: the method has
     * exactly one decision (the return expression), which must also be found and returned.
     */
    private List<Boolean> returnPolarities(String expression) throws IOException {
        Path source = srcRoot.resolve("Fixture.java");
        Files.writeString(source, RETURN_TEMPLATE.formatted(expression));

        analyzer.scan(srcRoot);

        ClassModel classModel = index.getClasses().get("Fixture");
        assertThat(classModel)
                .as("Fixture class not found in semantic index after scanning %s", source)
                .isNotNull();

        MethodModel method = classModel.getMethods().values().stream()
                .filter(m -> m.getName().equals("check"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("check() method not found in Fixture"));

        List<DecisionModel> decisions = method.getDecisionsList();
        assertThat(decisions)
                .as("Expected exactly one return-expression decision in check()")
                .hasSize(1);
        assertThat(decisions.get(0).kind())
                .as("Decision kind must be RETURN for a bare return expression")
                .isEqualTo("RETURN");

        return decisions.get(0).operands().stream()
                .map(OperandModel::jumpMeansTrue)
                .toList();
    }

    // =========================================================================
    // Simple base cases
    // =========================================================================

    /**
     * A single operand with no boolean connective. The initial context is {@code false}
     * (javac uses a negative opcode for the final check), so the only leaf also gets
     * {@code false}.
     *
     * <p>Example bytecode: {@code IFEQ else_label} for {@code if (a)}.</p>
     */
    @Test
    void singleOperand_false() throws IOException {
        assertThat(boolPolarities("a")).containsExactly(false);
    }

    /**
     * Two-operand OR: {@code a || b}.
     *
     * <ul>
     *   <li>{@code a} — left child of OR → javac jumps to then-body when a is TRUE
     *       → {@code jumpMeansTrue = true}</li>
     *   <li>{@code b} — right child (inherits initial context) → final check,
     *       negative opcode → {@code jumpMeansTrue = false}</li>
     * </ul>
     *
     * <p>Example bytecode:
     * {@code IFNE then_label} (for a), {@code IFEQ else_label} (for b).</p>
     */
    @Test
    void orTwoOperands_trueAndFalse() throws IOException {
        assertThat(boolPolarities("a || b")).containsExactly(true, false);
    }

    /**
     * Two-operand AND: {@code a && b}.
     *
     * <ul>
     *   <li>{@code a} — left child of AND → javac jumps to else-body when a is FALSE
     *       → {@code jumpMeansTrue = false}</li>
     *   <li>{@code b} — right child (inherits initial false) → {@code jumpMeansTrue = false}</li>
     * </ul>
     *
     * <p>Example bytecode:
     * {@code IFEQ else_label} (for a), {@code IFEQ else_label} (for b).</p>
     */
    @Test
    void andTwoOperands_bothFalse() throws IOException {
        assertThat(boolPolarities("a && b")).containsExactly(false, false);
    }

    // =========================================================================
    // Three-operand chains (tests associativity: left-associative by default)
    // =========================================================================

    /**
     * Three-operand OR chain: {@code a || b || c} (= {@code (a || b) || c}).
     *
     * <ul>
     *   <li>{@code a} — left of inner OR → true</li>
     *   <li>{@code b} — right of inner OR (which is left of outer OR) → true</li>
     *   <li>{@code c} — right of outer OR (inherits false) → false</li>
     * </ul>
     */
    @Test
    void orThreeOperands_trueTrueFalse() throws IOException {
        assertThat(boolPolarities("a || b || c")).containsExactly(true, true, false);
    }

    /**
     * Three-operand AND chain: {@code a && b && c} (= {@code (a && b) && c}).
     * All left-AND children have {@code jumpMeansTrue = false}; the right child
     * inherits {@code false} from the initial context.
     */
    @Test
    void andThreeOperands_allFalse() throws IOException {
        assertThat(boolPolarities("a && b && c")).containsExactly(false, false, false);
    }

    // =========================================================================
    // Mixed OR/AND trees
    // =========================================================================

    /**
     * Mixed precedence: {@code a || b && c} (= {@code a || (b && c)}).
     *
     * <ul>
     *   <li>{@code a} — left of top-level OR → true</li>
     *   <li>{@code b} — left of AND (which is OR's right child) → false</li>
     *   <li>{@code c} — right of AND (inherits OR's right context = false) → false</li>
     * </ul>
     *
     * <p>Verifies that AND within an OR sub-tree correctly overrides the OR context.</p>
     */
    @Test
    void orWithAndRight_trueFalseFalse() throws IOException {
        assertThat(boolPolarities("a || b && c")).containsExactly(true, false, false);
    }

    /**
     * Parenthesised OR inside AND: {@code (a || b) && c}.
     *
     * <p>javac compiles this as a flat jump sequence:</p>
     * <pre>
     *   IFNE_a  skip_b    (a is true → skip to c check)
     *   IFEQ_b  else      (b is false → go to else)
     *   skip_b:
     *   IFEQ_c  else      (c is false → go to else)
     *   then: ...
     * </pre>
     *
     * <ul>
     *   <li>{@code a} — left of OR (which is AND's left child); OR context gives true → true</li>
     *   <li>{@code b} — right of OR; inherits AND's left context = false → false</li>
     *   <li>{@code c} — right of AND (inherits initial false) → false</li>
     * </ul>
     *
     * <p>Verifies that the parent AND context correctly overrides the OR right-child
     * context for {@code b}.</p>
     */
    @Test
    void andWithOrLeft_trueFalseFalse() throws IOException {
        assertThat(boolPolarities("(a || b) && c")).containsExactly(true, false, false);
    }

    // =========================================================================
    // Null-check patterns — the exact scenarios from the IFNULL polarity bug
    // =========================================================================

    /**
     * {@code s == null || s.isBlank()} — the exact pattern that triggered the
     * IFNULL polarity bug (see {@code IFNULL_POLARITY_BUG.md}).
     *
     * <p>javac emits {@code IFNULL then_label} for {@code s == null}, so the jump
     * fires when the condition IS null = operand is TRUE → {@code jumpMeansTrue = true}.</p>
     *
     * <p>With the old code this would have been {@code [false, false]}, meaning the
     * TRUE/FALSE probe labels were swapped for {@code s == null}.</p>
     */
    @Test
    void nullEqualsOr_bugRegression_firstOperandTrue() throws IOException {
        assertThat(strPolarities("s == null || s.isBlank()")).containsExactly(true, false);
    }

    /**
     * {@code s != null && !s.isBlank()} — the AND-with-not-null pattern that was
     * "accidentally correct" before the fix.
     *
     * <p>javac emits {@code IFNULL else_label} for {@code s != null}, so the jump
     * fires when s IS null = {@code s != null} is FALSE → {@code jumpMeansTrue = false}.</p>
     *
     * <p>Must stay {@code [false, false]} after the fix.</p>
     */
    @Test
    void notNullAnd_accidentallyCorrect_bothOperandsFalse() throws IOException {
        assertThat(strPolarities("s != null && !s.isBlank()")).containsExactly(false, false);
    }

    /**
     * {@code s == null || s.isEmpty() || s.equals("x")} — three-operand OR with
     * a null check as the first operand. All non-last OR operands must be
     * {@code true}; the last inherits {@code false}.
     *
     * <p>Verifies that the OR-chain behaviour is stable when the first operand is
     * a null check (the riskiest pattern given the historical bug).</p>
     */
    @Test
    void nullEqualsOrChain_trueTrueFalse() throws IOException {
        assertThat(strPolarities("s == null || s.isEmpty() || s.equals(\"x\")"))
                .containsExactly(true, true, false);
    }

    /**
     * {@code s != null && s.length() > 0 && !s.isBlank()} — three-operand AND with
     * a not-null guard as the first operand (a common defensive pattern).
     * All operands must be {@code false}.
     */
    @Test
    void notNullAndChain_allOperandsFalse() throws IOException {
        assertThat(strPolarities("s != null && s.length() > 0 && !s.isBlank()"))
                .containsExactly(false, false, false);
    }

    // =========================================================================
    // Return-expression decisions — the bug reported in someLogic3
    // =========================================================================

    /**
     * {@code return obj != null;} — the exact pattern from the bug report.
     *
     * <p>javac compiles this as {@code IFNULL else_label}, which fires when obj IS null,
     * i.e. when {@code obj != null} is FALSE. The single operand must get
     * {@code jumpMeansTrue = false}.</p>
     *
     * <p>Before the fix, no {@code DecisionModel} existed for return statements,
     * causing the source-aware injector to fall back to the generic {@code "if (x == null)"}
     * opcode text with inverted polarity.</p>
     */
    @Test
    void returnNotNull_singleOperandFalse() throws IOException {
        assertThat(returnPolarities("obj != null")).containsExactly(false);
    }

    /**
     * {@code return obj == null;} — the inverse null check in a return.
     *
     * <p>javac compiles this as {@code IFNONNULL else_label}, which fires when obj is NOT
     * null, i.e. when {@code obj == null} is FALSE. The single operand must get
     * {@code jumpMeansTrue = false}.</p>
     */
    @Test
    void returnEqualsNull_singleOperandFalse() throws IOException {
        assertThat(returnPolarities("obj == null")).containsExactly(false);
    }

    /**
     * {@code return obj != null && obj.toString().length() > 0;} — compound return
     * expression. Same polarity rules as an {@code if} condition: both operands in an
     * AND chain are {@code false}.
     */
    @Test
    void returnCompoundAnd_bothOperandsFalse() throws IOException {
        assertThat(returnPolarities("obj != null && obj.toString().isEmpty()"))
                .containsExactly(false, false);
    }
}

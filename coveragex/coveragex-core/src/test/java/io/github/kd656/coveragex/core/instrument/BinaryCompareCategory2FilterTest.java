package io.github.kd656.coveragex.core.instrument;

import io.github.kd656.coveragex.core.analysis.source.impl.SourceCodeAnalyzer;
import io.github.kd656.coveragex.core.analysis.source.model.ClassModel;
import io.github.kd656.coveragex.core.analysis.source.model.DecisionModel;
import io.github.kd656.coveragex.core.analysis.source.model.MethodModel;
import io.github.kd656.coveragex.core.analysis.source.model.OperandModel;
import io.github.kd656.coveragex.core.analysis.source.model.SemanticIndex;
import com.github.javaparser.JavaParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the category-2 binary-compare literal filter in {@link SourceCodeAnalyzer}.
 *
 * <p>When either side of a binary comparison is a long or floating-point literal,
 * javac compiles the comparison as {@code LCMP}/{@code DCMPL}/{@code FCMPL} + {@code IF*}.
 * At the {@code IF*} site the stack holds the CMP result ({@code -1}/{@code 0}/{@code 1}),
 * not the original operand values. The analyser conservatively sets
 * {@link OperandModel#binaryCaptureMask()} to {@code 0} for such expressions so that
 * the bytecode emitter does not capture a mislabelled value.</p>
 *
 * <p>{@code char} comparisons (e.g. {@code c > 'a'}) compile to {@code IF_ICMP*} on
 * a category-1 int and are deliberately <em>not</em> filtered — they go through the
 * existing capture path correctly.</p>
 */
class BinaryCompareCategory2FilterTest {

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
    // Long-literal comparisons — filter must engage
    // =========================================================================

    /**
     * {@code x > 5L} — right-hand side is a long literal.
     * The analyser must set {@code binaryCaptureMask = 0} for this operand.
     */
    @Test
    void longLiteralRhs_captureMaskIsZero() throws IOException {
        int mask = analyseMask("long", "x > 5L");
        assertThat(mask)
                .as("long-literal rhs: binaryCaptureMask must be 0 (LCMP+IFLE pattern)")
                .isEqualTo(0);
    }

    /**
     * {@code 5L > x} — left-hand side is a long literal.
     * The analyser must set {@code binaryCaptureMask = 0} for this operand.
     */
    @Test
    void longLiteralLhs_captureMaskIsZero() throws IOException {
        int mask = analyseMask("long", "5L > x");
        assertThat(mask)
                .as("long-literal lhs: binaryCaptureMask must be 0 (LCMP+IFLE pattern)")
                .isEqualTo(0);
    }

    /**
     * Both sides are long literals: {@code 5L == 5L}.
     * The filter must engage (either side suffices).
     */
    @Test
    void bothLongLiterals_captureMaskIsZero() throws IOException {
        int mask = analyseMask("long", "5L == 5L");
        assertThat(mask)
                .as("both long-literal sides: binaryCaptureMask must be 0")
                .isEqualTo(0);
    }

    // =========================================================================
    // Double-literal comparisons — DoubleLiteralExpr covers double and float
    // =========================================================================

    /**
     * {@code x > 1.5} — right-hand side is a double literal ({@code DoubleLiteralExpr}).
     * The analyser must set {@code binaryCaptureMask = 0}.
     */
    @Test
    void doubleLiteralRhs_captureMaskIsZero() throws IOException {
        int mask = analyseMask("double", "x > 1.5");
        assertThat(mask)
                .as("double-literal rhs: binaryCaptureMask must be 0 (DCMPL+IFLE pattern)")
                .isEqualTo(0);
    }

    /**
     * {@code x > 1.5f} — float literal; JavaParser represents it as
     * {@code DoubleLiteralExpr} (the {@code f} suffix is in {@code getValue()},
     * no separate {@code FloatLiteralExpr} class). The filter must engage.
     */
    @Test
    void floatLiteralRhs_captureMaskIsZero() throws IOException {
        int mask = analyseMaskFloat("x > 1.5f");
        assertThat(mask)
                .as("float-literal rhs (DoubleLiteralExpr): binaryCaptureMask must be 0")
                .isEqualTo(0);
    }

    // =========================================================================
    // Non-category-2 comparisons — filter must NOT engage
    // =========================================================================

    /**
     * {@code x > 5} — integer literal rhs; compiles to {@code IF_ICMPGT}.
     * The filter must not engage; {@code x} (lhs) is capturable so mask = 1.
     */
    @Test
    void intLiteralRhs_captureMaskNonZero() throws IOException {
        int mask = analyseMask("int", "x > 5");
        assertThat(mask)
                .as("int-literal rhs must NOT be filtered; lhs 'x' is capturable → mask = 1")
                .isEqualTo(1);
    }

    /**
     * {@code x > y} — both sides are int variables; compiles to {@code IF_ICMPGT}.
     * Filter does not engage; mask = 3 (both sides capturable).
     */
    @Test
    void intVarVarCompare_captureMaskBothBits() throws IOException {
        int mask = analyseMaskTwoVars("int", "int", "x > y");
        assertThat(mask)
                .as("int var-vs-var must NOT be filtered; both sides capturable → mask = 3")
                .isEqualTo(3);
    }

    /**
     * {@code c > 'a'} — char literal; {@link com.github.javaparser.ast.expr.CharLiteralExpr}
     * is NOT a {@code DoubleLiteralExpr} or {@code LongLiteralExpr} — the filter must not
     * engage. Compiles to {@code IF_ICMPGT}; lhs {@code c} is capturable.
     */
    @Test
    void charLiteralRhs_filterDoesNotApply() throws IOException {
        int mask = analyseMask("char", "x > 'a'");
        assertThat(mask)
                .as("char-literal rhs must NOT be filtered (IF_ICMPGT, not FCMPL); "
                        + "lhs 'x' is capturable → mask = 1")
                .isEqualTo(1);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Writes a minimal Java source file containing {@code if (condition)} where
     * the parameter is named {@code x} with the given primitive type, analyses it
     * with {@link SourceCodeAnalyzer}, and returns the
     * {@link OperandModel#binaryCaptureMask()} of the single operand.
     *
     * @param paramType the Java primitive type of {@code x} (e.g. {@code "long"})
     * @param condition the binary expression to use in the {@code if} condition
     * @return the computed binary capture mask
     */
    private int analyseMask(String paramType, String condition) throws IOException {
        String src = """
                class Fixture {
                    boolean check(%s x) {
                        if (%s) return true;
                        return false;
                    }
                }
                """.formatted(paramType, condition);
        return analyseFirstOperandMask(src);
    }

    /**
     * Same as {@link #analyseMask} but for two-variable conditions ({@code x > y}).
     *
     * @param paramType1 type of first parameter (x)
     * @param paramType2 type of second parameter (y)
     * @param condition  the binary expression
     * @return the computed binary capture mask
     */
    private int analyseMaskTwoVars(String paramType1, String paramType2,
                                   String condition) throws IOException {
        String src = """
                class Fixture {
                    boolean check(%s x, %s y) {
                        if (%s) return true;
                        return false;
                    }
                }
                """.formatted(paramType1, paramType2, condition);
        return analyseFirstOperandMask(src);
    }

    /**
     * Variant for float comparisons where the parameter type is {@code float}.
     *
     * @param condition the float binary expression
     * @return the computed binary capture mask
     */
    private int analyseMaskFloat(String condition) throws IOException {
        return analyseMask("float", condition);
    }

    /**
     * Writes {@code src} to a temp file, scans it with the analyser, and returns
     * the {@link OperandModel#binaryCaptureMask()} of the first operand of the first
     * decision in the single method {@code check}.
     *
     * @param src the full Java source text
     * @return the binary capture mask of the first operand
     */
    private int analyseFirstOperandMask(String src) throws IOException {
        Path source = srcRoot.resolve("Fixture.java");
        Files.writeString(source, src);

        // Reset the index for each test (the @TempDir changes each time, but
        // the class name "Fixture" is reused — use a fresh SemanticIndex).
        SemanticIndex freshIndex = new SemanticIndex();
        SourceCodeAnalyzer freshAnalyzer = new SourceCodeAnalyzer(new JavaParser(), freshIndex);
        freshAnalyzer.scan(srcRoot);

        ClassModel classModel = freshIndex.getClasses().get("Fixture");
        assertThat(classModel)
                .as("Analyser must produce a ClassModel for 'Fixture'")
                .isNotNull();

        MethodModel method = classModel.getMethods().values().stream()
                .findFirst()
                .orElseThrow(() -> new AssertionError("No method found in Fixture"));
        assertThat(method.getDecisionsList())
                .as("Method must have at least one decision")
                .isNotEmpty();

        DecisionModel decision = method.getDecisionsList().get(0);
        assertThat(decision.operands())
                .as("Decision must have at least one operand")
                .isNotEmpty();

        OperandModel operand = decision.operands().get(0);
        return operand.binaryCaptureMask();
    }
}

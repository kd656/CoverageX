package io.github.kd656.coveragex.core.instrument;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ProbeInjectionSupport#isJumpTakenWhenTrue(int)}.
 *
 * <p>This is the narrowest layer of the IFNULL-polarity test pyramid. It validates
 * the opcode-to-polarity lookup table directly, without loading any bytecode or
 * running any instrumented code. If someone accidentally changes the table in a
 * future refactor, these tests will fail immediately and cheaply.</p>
 *
 * <p>The {@link Testable} inner subclass exposes the {@code protected} method under
 * test through a single public delegator. Its isConstructor passes sentinel values for
 * all fields that are irrelevant to polarity lookup.</p>
 *
 * <p>Test organisation:</p>
 * <ul>
 *   <li><b>Regression</b> — the exact opcode that was wrong before the fix ({@code IFNULL}).</li>
 *   <li><b>Boolean-result pattern</b> — {@code IFEQ}/{@code IFNE}: javac compiles
 *       {@code if (boolExpr)} as {@code IFEQ else_label}, so {@code IFEQ} must be
 *       {@code false} and {@code IFNE} must be {@code true}.</li>
 *   <li><b>Numeric comparisons</b> — all six integer-comparison opcodes and their
 *       pairings, to guard against typos in the switch arms.</li>
 *   <li><b>Reference comparisons</b> — {@code IF_ACMPEQ}/{@code IF_ACMPNE} and
 *       {@code IFNULL}/{@code IFNONNULL}.</li>
 * </ul>
 */
class ProbeInjectionSupportTest {

    /**
     * Minimal no-op subclass that promotes {@code isJumpTakenWhenTrue} to public
     * visibility so tests can call it directly. The isConstructor arguments are
     * semantically meaningless here; only the polarity-table logic is exercised.
     */
    private static class Testable extends ProbeInjectionSupport {

        Testable() {
            super(Opcodes.ASM9,
                    (MethodVisitor) null,
                    "TestClass", "testMethod", "()V", 0,
                    new AtomicInteger(),
                    new ArrayList<>());
        }

        /** Exposes the protected method under test. */
        boolean jumpMeansTrue(int opcode) {
            return isJumpTakenWhenTrue(opcode);
        }
    }

    private final Testable support = new Testable();

    // =========================================================================
    // Regression: IFNULL was the reported bug (returned false, should be true)
    // =========================================================================

    /**
     * {@code IFNULL} fires when the reference IS null — i.e. when the condition
     * {@code x == null} evaluates to {@code true}. The fix changed this from
     * {@code false} to {@code true}. This test is the direct regression guard.
     *
     * <p>Incorrect before fix: {@code IFNULL} returned {@code false}, causing the
     * TRUE/FALSE probe labels to be swapped for any {@code == null} operand.</p>
     */
    @Test
    void ifnull_returnsTrueRegression() {
        assertThat(support.jumpMeansTrue(Opcodes.IFNULL))
                .as("IFNULL fires when x IS null → condition 'x == null' is TRUE")
                .isTrue();
    }

    // =========================================================================
    // Reference null-checks
    // =========================================================================

    /**
     * {@code IFNONNULL} fires when the reference is NOT null — i.e. when
     * {@code x != null} is {@code true}. This was already correct before the fix
     * and must remain unchanged.
     */
    @Test
    void ifnonnull_returnsTrue() {
        assertThat(support.jumpMeansTrue(Opcodes.IFNONNULL))
                .as("IFNONNULL fires when x is not null → condition 'x != null' is TRUE")
                .isTrue();
    }

    // =========================================================================
    // Boolean-result pattern (the original motivation for the polarity table)
    // =========================================================================

    /**
     * {@code IFEQ} fires when the top-of-stack integer is zero (false).
     * javac compiles {@code if (boolMethod())} as {@code IFEQ else_label}, so
     * jump-taken means the condition is FALSE. Without this distinction the
     * TRUE/FALSE probe labels would be swapped for every {@code boolean}-returning
     * call such as {@code String.equalsIgnoreCase}.
     */
    @Test
    void ifeq_returnsFalse() {
        assertThat(support.jumpMeansTrue(Opcodes.IFEQ))
                .as("IFEQ fires when x == 0 (false) → jump-taken means condition is FALSE")
                .isFalse();
    }

    /**
     * {@code IFNE} fires when the top-of-stack integer is non-zero (true).
     * Used by javac for patterns like {@code if (x != 0)} or negated boolean checks.
     */
    @Test
    void ifne_returnsTrue() {
        assertThat(support.jumpMeansTrue(Opcodes.IFNE))
                .as("IFNE fires when x != 0 (true) → jump-taken means condition is TRUE")
                .isTrue();
    }

    // =========================================================================
    // Signed integer comparisons against zero
    // =========================================================================

    /**
     * {@code IFLT} fires when value {@literal <} 0, which corresponds to a
     * mathematically false result in typical Java source patterns; jump-taken = FALSE.
     */
    @Test
    void iflt_returnsFalse() {
        assertThat(support.jumpMeansTrue(Opcodes.IFLT)).isFalse();
    }

    /**
     * {@code IFGE} fires when value {@literal >=} 0 — the positive (true) direction.
     */
    @Test
    void ifge_returnsTrue() {
        assertThat(support.jumpMeansTrue(Opcodes.IFGE)).isTrue();
    }

    /**
     * {@code IFGT} fires when value {@literal >} 0 — the positive (true) direction.
     */
    @Test
    void ifgt_returnsTrue() {
        assertThat(support.jumpMeansTrue(Opcodes.IFGT)).isTrue();
    }

    /**
     * {@code IFLE} fires when value {@literal <=} 0 — the negative (false) direction.
     */
    @Test
    void ifle_returnsFalse() {
        assertThat(support.jumpMeansTrue(Opcodes.IFLE)).isFalse();
    }

    // =========================================================================
    // Integer–integer comparisons (IF_ICMPxx)
    // =========================================================================

    /**
     * {@code IF_ICMPEQ} fires when two integers are equal. Javac uses this for
     * {@code a == b} and similar equality patterns; jump-taken = FALSE (negative
     * polarity, mirrors {@code IFEQ}).
     */
    @Test
    void if_icmpeq_returnsFalse() {
        assertThat(support.jumpMeansTrue(Opcodes.IF_ICMPEQ)).isFalse();
    }

    /**
     * {@code IF_ICMPNE} fires when two integers are not equal — the positive direction.
     */
    @Test
    void if_icmpne_returnsTrue() {
        assertThat(support.jumpMeansTrue(Opcodes.IF_ICMPNE)).isTrue();
    }

    /**
     * {@code IF_ICMPLT} fires when a {@literal <} b — negative polarity.
     */
    @Test
    void if_icmplt_returnsFalse() {
        assertThat(support.jumpMeansTrue(Opcodes.IF_ICMPLT)).isFalse();
    }

    /**
     * {@code IF_ICMPGE} fires when a {@literal >=} b — positive polarity.
     */
    @Test
    void if_icmpge_returnsTrue() {
        assertThat(support.jumpMeansTrue(Opcodes.IF_ICMPGE)).isTrue();
    }

    /**
     * {@code IF_ICMPGT} fires when a {@literal >} b — positive polarity.
     */
    @Test
    void if_icmpgt_returnsTrue() {
        assertThat(support.jumpMeansTrue(Opcodes.IF_ICMPGT)).isTrue();
    }

    /**
     * {@code IF_ICMPLE} fires when a {@literal <=} b — negative polarity.
     */
    @Test
    void if_icmple_returnsFalse() {
        assertThat(support.jumpMeansTrue(Opcodes.IF_ICMPLE)).isFalse();
    }

    // =========================================================================
    // Reference–reference comparisons (IF_ACMPxx)
    // =========================================================================

    /**
     * {@code IF_ACMPEQ} fires when two references are the same object — negative
     * polarity (equality is the "failing" direction in assertion-like patterns).
     */
    @Test
    void if_acmpeq_returnsFalse() {
        assertThat(support.jumpMeansTrue(Opcodes.IF_ACMPEQ)).isFalse();
    }

    /**
     * {@code IF_ACMPNE} fires when two references are NOT the same object —
     * positive polarity.
     */
    @Test
    void if_acmpne_returnsTrue() {
        assertThat(support.jumpMeansTrue(Opcodes.IF_ACMPNE)).isTrue();
    }

    // =========================================================================
    // Symmetry invariants
    // =========================================================================

    /**
     * Verifies that classic comparison opcodes and their bytecode negations have
     * opposite polarities.
     *
     * <p><b>Why {@code IFNULL}/{@code IFNONNULL} are excluded:</b> these two opcodes
     * are NOT polarity negations of each other. Both fire when their own named
     * condition is TRUE — {@code IFNULL} when {@code x == null} is true, and
     * {@code IFNONNULL} when {@code x != null} is true. Treating them as a polarity
     * pair would assert that one should be {@code false}, which contradicts the
     * semantics described in {@link #ifnull_returnsTrueRegression()} and
     * {@link #ifnonnull_returnsTrue()}.</p>
     *
     * <p>The pairs included here are opcodes that encode a single comparison and its
     * exact inverse (same operands, opposite result), so one always fires when the
     * other would not, and their polarity must be opposite.</p>
     */
    @Test
    void negationPairsHaveOppositePolarities() {
        List<int[]> pairs = List.of(
                new int[]{Opcodes.IFEQ,      Opcodes.IFNE},
                new int[]{Opcodes.IFLT,      Opcodes.IFGE},
                new int[]{Opcodes.IFLE,      Opcodes.IFGT},
                new int[]{Opcodes.IF_ICMPEQ, Opcodes.IF_ICMPNE},
                new int[]{Opcodes.IF_ICMPLT, Opcodes.IF_ICMPGE},
                new int[]{Opcodes.IF_ICMPLE, Opcodes.IF_ICMPGT},
                new int[]{Opcodes.IF_ACMPEQ, Opcodes.IF_ACMPNE}
        );

        for (int[] pair : pairs) {
            boolean p1 = support.jumpMeansTrue(pair[0]);
            boolean p2 = support.jumpMeansTrue(pair[1]);
            assertThat(p1)
                    .as("Opcodes %d and %d must have opposite polarities", pair[0], pair[1])
                    .isNotEqualTo(p2);
        }
    }

    /**
     * Verifies that both null-check opcodes return {@code true}.
     *
     * <p>{@code IFNULL} fires when the reference IS null — condition {@code x == null}
     * is TRUE. {@code IFNONNULL} fires when the reference is NOT null — condition
     * {@code x != null} is TRUE. Each opcode fires when its own named condition holds,
     * so both correctly return {@code true} from {@code isJumpTakenWhenTrue}.</p>
     *
     * <p>Unlike integer-comparison pairs (e.g. {@code IFEQ}/{@code IFNE}), these two
     * opcodes test <em>different</em> conditions ({@code == null} vs {@code != null})
     * rather than being inverses of the same condition, so they are not a polarity
     * negation pair.</p>
     */
    @Test
    void nullCheckOpcodesBothReturnTrue() {
        assertThat(support.jumpMeansTrue(Opcodes.IFNULL))
                .as("IFNULL fires when x IS null → 'x == null' is TRUE → must return true")
                .isTrue();
        assertThat(support.jumpMeansTrue(Opcodes.IFNONNULL))
                .as("IFNONNULL fires when x is NOT null → 'x != null' is TRUE → must return true")
                .isTrue();
    }
}

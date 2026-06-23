package io.github.kd656.coveragex.api.data;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins the stable integer codec for {@link OperandKind}.
 *
 * <p><b>Do not change the assertions in {@link #codeAssignmentsArePinned()} without
 * migrating existing {@code .exec} files.</b> Those values are persisted to disk and
 * read back across JVM restarts; changing them silently corrupts any file produced by
 * an older version of the tool.</p>
 */
class OperandKindCodecTest {

    /**
     * Every constant must have a code that is unique across all constants.
     * Duplicate codes would make {@link OperandKind#fromCode(int)} non-deterministic.
     */
    @Test
    void everyConstantHasUniqueCode() {
        Map<Integer, OperandKind> seen = new HashMap<>();
        for (OperandKind k : OperandKind.values()) {
            OperandKind clash = seen.put(k.code(), k);
            assertThat(clash)
                    .as("code %d is shared by %s and %s", k.code(), k, clash)
                    .isNull();
        }
    }

    /**
     * {@link OperandKind#fromCode(int)} must return the constant whose
     * {@link OperandKind#code()} equals the supplied integer.
     */
    @Test
    void roundTripsByCode() {
        for (OperandKind k : OperandKind.values()) {
            assertThat(OperandKind.fromCode(k.code()))
                    .as("round-trip for %s (code %d)", k, k.code())
                    .isEqualTo(k);
        }
    }

    /**
     * The code assignments are a persisted contract. Changing this test requires
     * migrating existing {@code .exec} files; do not alter the expected values here
     * without a deliberate migration plan.
     */
    @Test
    void codeAssignmentsArePinned() {
        assertThat(OperandKind.METHOD_CALL.code()).isEqualTo(1);
        assertThat(OperandKind.BINARY_COMPARE.code()).isEqualTo(2);
        assertThat(OperandKind.UNARY.code()).isEqualTo(3);
        assertThat(OperandKind.BARE_REFERENCE.code()).isEqualTo(4);
        assertThat(OperandKind.UNKNOWN.code()).isEqualTo(0);
    }

    /**
     * An integer that does not correspond to any constant must produce an
     * {@link IllegalArgumentException}, not a silent {@code null} or an
     * {@link ArrayIndexOutOfBoundsException}.
     */
    @Test
    void unknownCodeThrows() {
        assertThatThrownBy(() -> OperandKind.fromCode(999))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("999");
    }
}

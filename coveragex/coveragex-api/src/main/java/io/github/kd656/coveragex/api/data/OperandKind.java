package io.github.kd656.coveragex.api.data;

import java.util.Arrays;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

/**
 * Structural classification of a boolean operand within a conditional expression.
 * Drives both the report-side column-header rendering and the bytecode-side capture
 * strategy.
 *
 * <p>The value is derived statically from the JavaParser AST node type during the
 * source analysis phase and carried through to the binary format so that the report
 * renderer does not need access to source code at reporting time.</p>
 *
 * <p>Each constant carries a stable {@link #code()} integer that is used when
 * serialising to the binary {@code .exec} format. Codes are assigned by hand and
 * must never be reused or renumbered — doing so would silently corrupt {@code .exec}
 * files written by an older version of the tool. New constants must be assigned a
 * fresh code; {@code UNKNOWN = 0} is reserved as the default for forward-compatible
 * readers encountering an unrecognised value.</p>
 */
public enum OperandKind {

    /** Method invocation, e.g. {@code x.foo(y, z)} or {@code name.startsWith(".")}. */
    METHOD_CALL(1),

    /** Binary comparison, e.g. {@code a == b} or {@code x > 5}. */
    BINARY_COMPARE(2),

    /** Unary negation, e.g. {@code !x}. */
    UNARY(3),

    /** Bare boolean reference, e.g. a local variable or field read without a call. */
    BARE_REFERENCE(4),

    /** Operand that the analyzer could not classify into any of the above kinds. */
    UNKNOWN(0);

    private static final Map<Integer, OperandKind> BY_CODE =
            Arrays.stream(values()).collect(toMap(OperandKind::code, k -> k));

    private final int code;

    OperandKind(int code) {
        this.code = code;
    }

    /**
     * Returns the stable integer code used when serialising this constant to the
     * binary {@code .exec} format.
     *
     * @return the stable integer code for this constant
     */
    public int code() {
        return code;
    }

    /**
     * Returns the {@code OperandKind} whose {@link #code()} equals {@code code}.
     *
     * @param code the integer code read from a binary {@code .exec} stream
     * @return the matching constant
     * @throws IllegalArgumentException if no constant has the given code
     */
    public static OperandKind fromCode(int code) {
        OperandKind kind = BY_CODE.get(code);
        if (kind == null) {
            throw new IllegalArgumentException("Unknown OperandKind code: " + code);
        }
        return kind;
    }
}

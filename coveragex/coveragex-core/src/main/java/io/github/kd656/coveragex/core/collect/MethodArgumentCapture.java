package io.github.kd656.coveragex.core.collect;

import java.util.ArrayList;
import java.util.List;

/**
 * Captures a bounded, safe snapshot of raw method arguments at probe invocation time.
 *
 * <p>Called on every instrumented method entry or branch probe, so it must never throw.
 * It enforces two hard limits to keep the collector overhead predictable: at most
 * {@value #MAX_ARGS} arguments are captured, and each argument string is capped at
 * {@value #MAX_ARG_LEN} characters. Arguments whose {@code toString()} throws or
 * recurses infinitely are replaced with a diagnostic message rather than propagating
 * the failure.</p>
 *
 * <p><b>Capture-time rule (§7.1):</b> the reference-typed payload must never be
 * retained beyond this call. Both {@link #capture(Object[])} and
 * {@link #singleValue(Object)} convert to {@link String} immediately so the tracker
 * stores only the textual form, independent of the source object's lifetime or any
 * future mutation of its state.</p>
 */
public final class MethodArgumentCapture {

    /** Maximum number of argument values captured per probe invocation. */
    public static final int MAX_ARGS    = 16;

    /** Maximum number of characters retained per captured argument string. */
    public static final int MAX_ARG_LEN = 256;

    private MethodArgumentCapture() {}

    /**
     * Converts a single operand value to its serialized form.
     *
     * <p>Equivalent to a one-element {@link #capture(Object[])} call; provided
     * so branch-operand capture sites avoid allocating a temporary array. The
     * same truncation and {@code Throwable}-catch rules apply.</p>
     *
     * @param value the raw operand value, or {@code null}
     * @return the serialized form, never {@code null}
     */
    public static SerializedArg singleValue(Object value) {
        if (value == null) {
            return SerializedArg.ofNull();
        }
        String s;
        try {
            s = String.valueOf(value);
        } catch (Throwable t) {
            s = fallbackMessage(t);
        }
        if (s.length() > MAX_ARG_LEN) {
            s = s.substring(0, MAX_ARG_LEN) + "…";
        }
        return SerializedArg.of(s);
    }

    /**
     * Captures up to {@value #MAX_ARGS} elements from {@code args}, converting
     * each to its string form via {@link #singleValue(Object)}.
     *
     * @param args the raw argument array, may be {@code null} or empty
     * @return an immutable list of serialized values, never {@code null}
     */
    public static List<SerializedArg> capture(Object[] args) {
        if (args == null || args.length == 0) {
            return List.of();
        }
        int count = Math.min(args.length, MAX_ARGS);
        List<SerializedArg> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add(singleValue(args[i]));
        }
        return List.copyOf(result);
    }

    private static String fallbackMessage(Throwable t) {
        String msg = t.getLocalizedMessage();
        return msg != null ? msg
                : "Error occurred while serializing arguments in " + t.getClass().getSimpleName();
    }
}

package com.coveragex.core.collect;

import java.util.ArrayList;
import java.util.List;

/**
 * Captures a bounded, safe snapshot of raw method arguments at probe invocation time.
 *
 * <p>Called on every instrumented method entry, so it must never throw. It enforces two
 * hard limits to keep the collector overhead predictable: at most {@value #MAX_ARGS}
 * arguments are captured, and each argument string is capped at {@value #MAX_ARG_LEN}
 * characters. Arguments whose {@code toString()} throws or recurses infinitely are
 * replaced with a diagnostic message rather than propagating the failure.</p>
 */
public final class MethodArgumentCapture {

    public static final int MAX_ARGS    = 16;
    public static final int MAX_ARG_LEN = 256;

    private MethodArgumentCapture() {}

    public static List<SerializedArg> capture(Object[] args) {
        if (args == null || args.length == 0) return List.of();
        int count = Math.min(args.length, MAX_ARGS);
        List<SerializedArg> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Object a = args[i];
            if (a == null) {
                result.add(SerializedArg.ofNull());
            } else {
                String s;
                try {
                    s = String.valueOf(a);
                } catch (Throwable t) {
                    // TODO: Improve
                    String msg = t.getLocalizedMessage();
                    s = msg != null ? msg : "Error occurred while serializing arguments in " + t.getClass().getSimpleName();
                }
                if (s.length() > MAX_ARG_LEN) s = s.substring(0, MAX_ARG_LEN) + "…";
                result.add(SerializedArg.of(s));
            }
        }
        return List.copyOf(result);
    }
}

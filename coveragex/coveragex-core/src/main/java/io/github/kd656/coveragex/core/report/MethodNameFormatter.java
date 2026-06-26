package io.github.kd656.coveragex.core.report;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Translates JVM-internal method name forms into human-readable display strings
 * for the coverage report UI.
 *
 * <p>The stored form in binary files ({@code .exec}) and in all
 * {@link io.github.kd656.coveragex.api.data.ProbeMetadata} records is always the
 * raw JVM name. This class transforms names only on the display side.</p>
 *
 * <p>Mapping rules:
 * <table border="1">
 * <tr><th>JVM name</th><th>Display string</th></tr>
 * <tr><td>{@code <init>}</td><td>{@code Constructor}</td></tr>
 * <tr><td>{@code <clinit>}</td><td>{@code Static initialiser}</td></tr>
 * <tr><td>{@code lambda$method$0}</td><td>{@code method (lambda #1)}</td></tr>
 * <tr><td>{@code lambda$method$1}</td><td>{@code method (lambda #2)}</td></tr>
 * <tr><td>{@code access$N}</td><td>{@code synthetic accessor}</td></tr>
 * <tr><td><i>anything else</i></td><td><i>unchanged</i></td></tr>
 * </table>
 * </p>
 *
 * <p>Lambda indices are shifted by one (0-based JVM index → 1-based human
 * count) so the UI reads "lambda #1" rather than "lambda #0".</p>
 */
public final class MethodNameFormatter {

    /** Pattern matching {@code lambda$<method>$<index>}. */
    private static final Pattern LAMBDA_PATTERN =
            Pattern.compile("lambda\\$(.+)\\$(\\d+)");

    /** Pattern matching {@code access$<digits>}. */
    private static final Pattern ACCESS_PATTERN =
            Pattern.compile("access\\$\\d+");

    private MethodNameFormatter() {
        // utility class — do not instantiate
    }

    /**
     * Converts a JVM method name into its human-readable display form.
     *
     * @param jvmName the raw JVM method name; must not be {@code null}
     * @return a display-friendly string; never {@code null}
     */
    public static String format(String jvmName) {
        if (jvmName == null) {
            return "";
        }

        switch (jvmName) {
            case "<init>":
                return "Constructor";
            case "<clinit>":
                return "Static initialiser";
            default:
                break;
        }

        Matcher lambdaMatcher = LAMBDA_PATTERN.matcher(jvmName);
        if (lambdaMatcher.matches()) {
            String enclosingMethod = lambdaMatcher.group(1);
            int index = Integer.parseInt(lambdaMatcher.group(2));
            return enclosingMethod + " (lambda #" + (index + 1) + ")";
        }

        if (ACCESS_PATTERN.matcher(jvmName).matches()) {
            return "synthetic accessor";
        }

        return jvmName;
    }
}

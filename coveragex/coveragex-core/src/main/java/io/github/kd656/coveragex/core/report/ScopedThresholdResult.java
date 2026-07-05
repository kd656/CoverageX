package io.github.kd656.coveragex.core.report;

/**
 * Per-scope outcome of a threshold check.
 *
 * <p>The {@code scopeId} is the sanitized scope identifier ({@code "dto"},
 * {@code "service"}, {@code "api-2"}), or the literal {@code "global"} for the
 * merged-reactor evaluation produced by {@link #global}.</p>
 */
public record ScopedThresholdResult(
        String scopeId,
        String displayName,
        CoverageThresholdChecker.ThresholdResult threshold
) {

    private static final String GLOBAL_SCOPE_ID = "global";
    private static final String GLOBAL_DISPLAY_NAME = "All modules";

    /** Sentinel for the merged-reactor result under {@link ThresholdMode#GLOBAL}. */
    public static ScopedThresholdResult global(CoverageThresholdChecker.ThresholdResult threshold) {
        return new ScopedThresholdResult(GLOBAL_SCOPE_ID, GLOBAL_DISPLAY_NAME, threshold);
    }

    public boolean passed() {
        return threshold.passed();
    }
}

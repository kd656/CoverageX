package io.github.kd656.coveragex.core.report;

/**
 * How the aggregator applies the {@code minimumCoverage} threshold across multiple
 * scopes.
 *
 * <ul>
 *   <li>{@link #GLOBAL} — merge every scope's execution data into one snapshot and
 *       check the threshold against the reactor-wide total. A high-coverage module
 *       can offset a low-coverage one.</li>
 *   <li>{@link #PER_MODULE} — check every scope independently. Any module below the
 *       threshold fails the build. Useful when a new module must not slip in with
 *       low coverage hidden behind a healthy reactor average.</li>
 * </ul>
 */
public enum ThresholdMode {
    GLOBAL,
    PER_MODULE
}

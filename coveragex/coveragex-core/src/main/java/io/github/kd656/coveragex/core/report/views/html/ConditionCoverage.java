package io.github.kd656.coveragex.core.report.views.html;

/**
 * Aggregate coverage state for one direction (TRUE or FALSE) across all conditions
 * on a single source line. A line with a compound expression such as
 * {@code a == null || b.isEmpty()} may have multiple conditions; this enum
 * summarises how many of them are covered in the given direction.
 */
public enum ConditionCoverage {
    /** Every condition on the line is covered in this direction. */
    ALL,
    /** At least one condition is covered in this direction, but at least one is not. */
    PARTIAL,
    /** No condition on the line is covered in this direction. */
    NONE
}

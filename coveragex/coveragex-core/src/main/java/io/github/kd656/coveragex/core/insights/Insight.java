package io.github.kd656.coveragex.core.insights;

/**
 * A single actionable coverage insight produced by {@link InsightEngine}.
 *
 * @param classId    the internal class identifier (e.g. {@code "com/example/OrderService"})
 * @param methodName the method this insight pertains to; {@code null} for class-level insights
 * @param line       source line number, or {@code -1} for class-level insights
 * @param id         stable rule identifier (e.g. {@code "DEAD_METHOD"}, {@code "MISSING_BRANCH_FALSE"})
 * @param severity   how urgent the insight is
 * @param message    human-readable one-line summary
 * @param hint       actionable suggestion to resolve the issue; may be empty but never {@code null}
 */
public record Insight(
        String classId,
        String methodName,
        int line,
        String id,
        Severity severity,
        String message,
        String hint
) {}

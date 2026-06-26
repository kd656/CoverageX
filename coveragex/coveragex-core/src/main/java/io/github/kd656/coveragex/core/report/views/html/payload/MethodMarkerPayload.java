package io.github.kd656.coveragex.core.report.views.html.payload;

import com.alibaba.fastjson2.annotation.JSONField;
import java.util.List;

/**
 * Compact JSON payload for a method entry marker on a source line.
 *
 * <p>When {@code totalCallCount} is 0 the renderer shows a dead-code label
 * instead of a clickable marker. When {@code parameterNames} is non-empty, the
 * invocation popover renders one column per parameter name instead of the
 * single aggregated "Arguments" column.</p>
 *
 * @param methodName     simple method name shown in the popover title
 * @param totalCallCount total invocation count across all test runs
 * @param invocations    per-test-invocation breakdown; {@code null} when empty
 * @param parameterNames source-level parameter names in declaration order;
 *                       {@code null} or empty triggers the fallback single-column
 *                       rendering
 */
public record MethodMarkerPayload(
    @JSONField(name = "n")      String methodName,
    @JSONField(name = "t")      int totalCallCount,
    @JSONField(name = "inv")    List<InvocationPayload> invocations,
    @JSONField(name = "params") List<String> parameterNames
) {}

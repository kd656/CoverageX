package io.github.kd656.coveragex.core.report.views.html.payload;

import com.alibaba.fastjson2.annotation.JSONField;
import java.util.List;

/**
 * Compact JSON payload for a method entry marker on a source line.
 * When {@code totalCallCount} is 0, the renderer shows a dead-code label instead of a clickable marker.
 */
public record MethodMarkerPayload(
    @JSONField(name = "n")   String methodName,
    @JSONField(name = "t")   int totalCallCount,
    @JSONField(name = "inv") List<InvocationPayload> invocations
) {}

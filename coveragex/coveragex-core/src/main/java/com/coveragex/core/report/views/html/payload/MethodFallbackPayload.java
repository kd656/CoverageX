package com.coveragex.core.report.views.html.payload;

import com.alibaba.fastjson2.annotation.JSONField;

/**
 * Compact JSON payload for a method fallback row (used when no source file is found).
 * For methods: {@code name} is the method name; {@code displayText} is "Nx" or "never called".
 * For branches: {@code name} is "condition [T]" or "condition [F]"; {@code displayText} is "hit" or "miss".
 */
public record MethodFallbackPayload(
    @JSONField(name = "n")  String name,
    @JSONField(name = "h")  int hit,
    @JSONField(name = "st") String displayText
) {}

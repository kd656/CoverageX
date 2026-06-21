package com.coveragex.core.report.views.html.payload;

import com.alibaba.fastjson2.annotation.JSONField;

/**
 * Compact JSON payload for one coverage insight.
 * Severity codes: "C"=CRITICAL, "W"=WARNING, "I"=INFO, "P"=POSITIVE.
 * {@code lineNumber} is 0 when the insight is not line-specific.
 */
public record InsightPayload(
    @JSONField(name = "sev")  String severityCode,
    @JSONField(name = "msg")  String message,
    @JSONField(name = "ref")  String codeReference,
    @JSONField(name = "hint") String hint,
    @JSONField(name = "line") int lineNumber
) {}

package io.github.kd656.coveragex.core.report.views.html.payload;

import com.alibaba.fastjson2.annotation.JSONField;

/**
 * Compact JSON payload for one test that exercised a branch direction.
 */
public record BranchTestPayload(
    @JSONField(name = "t")   String testMethodName,
    @JSONField(name = "cls") String testClassName,
    @JSONField(name = "cnt") int count
) {}

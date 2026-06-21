package com.coveragex.core.report.views.html.payload;

import com.alibaba.fastjson2.annotation.JSONField;
import java.util.List;

/**
 * Compact JSON payload for one branch badge (one direction: TRUE or FALSE).
 * {@code direction} = 1 for TRUE, 0 for FALSE.
 * {@code aggregateCoverage} = 2 ALL, 1 PARTIAL, 0 NONE.
 */
public record BranchBadgePayload(
    @JSONField(name = "d")     int direction,
    @JSONField(name = "cov")   int aggregateCoverage,
    @JSONField(name = "conds") List<BranchConditionPayload> conditions,
    @JSONField(name = "tests") List<BranchTestPayload> tests
) {}

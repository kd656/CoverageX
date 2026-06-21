package io.github.kd656.coveragex.core.report.views.html.payload;

import com.alibaba.fastjson2.annotation.JSONField;

/**
 * Compact JSON payload for one boolean sub-expression within a branch.
 * Wire names are single letters to minimise file size.
 */
public record BranchConditionPayload(
    @JSONField(name = "t")  String conditionText,
    @JSONField(name = "th") int trueHit,
    @JSONField(name = "fh") int falseHit,
    @JSONField(name = "tc") int trueCount,
    @JSONField(name = "fc") int falseCount,
    @JSONField(name = "thi") String trueHint,
    @JSONField(name = "fhi") String falseHint
) {}

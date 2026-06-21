package com.coveragex.core.report.views.html.payload;

import com.alibaba.fastjson2.annotation.JSONField;
import java.util.List;

/**
 * Compact JSON payload for one source line.
 * Optional fields use null/empty so FastJson2 can omit them with NotWriteDefaultValue.
 *
 * Coverage codes: 0=NOT_EXECUTABLE, 1=HIT, 2=MISS, 3=PARTIAL_BRANCH
 */
public record SourceLinePayload(
    @JSONField(name = "n")   int lineNumber,
    @JSONField(name = "s")   String sourceText,
    @JSONField(name = "c")   int coverageCode,
    @JSONField(name = "h")   String hitCountDisplay,
    @JSONField(name = "sep") Integer methodSeparatorBefore,
    @JSONField(name = "mm")  MethodMarkerPayload methodMarker,
    @JSONField(name = "bb")  List<BranchBadgePayload> branchBadges
) {}

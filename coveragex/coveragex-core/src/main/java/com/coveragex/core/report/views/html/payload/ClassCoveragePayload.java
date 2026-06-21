package com.coveragex.core.report.views.html.payload;

import com.alibaba.fastjson2.annotation.JSONField;
import java.util.List;

/**
 * Root JSON payload object for a single class's coverage data file.
 * Serialized by FastJson2 and wrapped in a CoverageX.registerClass() call.
 *
 * Either {@code sourceLines} or {@code methodFallbackRows} is non-null — never both.
 */
public record ClassCoveragePayload(
    @JSONField(name = "name")   String simpleName,
    @JSONField(name = "pkg")    String packageName,
    @JSONField(name = "pct")    double coveragePercent,
    @JSONField(name = "crits")  int criticalInsightCount,
    @JSONField(name = "warns")  int warningInsightCount,
    @JSONField(name = "infos")  int infoInsightCount,
    @JSONField(name = "pos")    int positiveInsightCount,
    @JSONField(name = "lines")  List<SourceLinePayload> sourceLines,
    @JSONField(name = "methods") List<MethodFallbackPayload> methodFallbackRows,
    @JSONField(name = "insights") List<InsightPayload> insights
) {}

package io.github.kd656.coveragex.core.report.views.html.payload;

import com.alibaba.fastjson2.annotation.JSONField;

import java.util.List;

/**
 * Compact JSON payload for one boolean sub-expression within a branch.
 * Wire names are single letters to minimise file size.
 *
 * @param conditionText verbatim source text of the operand
 * @param trueHit       1 if the TRUE direction was taken, 0 otherwise
 * @param falseHit      1 if the FALSE direction was taken, 0 otherwise
 * @param trueCount     hit count for the TRUE direction
 * @param falseCount    hit count for the FALSE direction
 * @param trueHint      human-readable hint for the TRUE direction
 * @param falseHint     human-readable hint for the FALSE direction
 * @param trueTests     tests that exercised the TRUE direction
 * @param falseTests    tests that exercised the FALSE direction
 * @param operandArgs   column-label schema (one entry per non-literal operand
 *                      argument, in source order) for the per-direction test
 *                      tables; empty when no source map is available
 */
public record BranchConditionPayload(
        @JSONField(name = "t")   String conditionText,
        @JSONField(name = "th")  int trueHit,
        @JSONField(name = "fh")  int falseHit,
        @JSONField(name = "tc")  int trueCount,
        @JSONField(name = "fc")  int falseCount,
        @JSONField(name = "thi") String trueHint,
        @JSONField(name = "fhi") String falseHint,
        @JSONField(name = "tt")  List<BranchTestPayload> trueTests,
        @JSONField(name = "ft")  List<BranchTestPayload> falseTests,
        @JSONField(name = "ops") List<String> operandArgs) {
}

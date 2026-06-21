package io.github.kd656.coveragex.core.report;

import io.github.kd656.coveragex.api.data.ProbeMetadata;
import io.github.kd656.coveragex.api.data.ProbeMetadataVisitor;

/**
 * Generates a heuristic hint line for each uncovered probe, guiding the developer
 * towards a useful test case.
 *
 * <p>Hint rules:
 * <ul>
 *   <li>METHOD: generic boundary-input advice.</li>
 *   <li>BRANCH: condition-text heuristics (null check, zero check, isEmpty, etc.).</li>
 *   <li>RETURN: generic "exercise this return path" advice.</li>
 *   <li>THROW:  generic "exercise this exception path" advice.</li>
 * </ul>
 * </p>
 *
 * <p>All dispatch is handled by the visitor pattern — no {@code instanceof} or
 * type-switch statements appear in this class.</p>
 */
public class ProbeMetadataHintGenerator implements ProbeMetadataVisitor<String> {

    @Override
    public String visit(ProbeMetadata.MethodProbe p) {
        return "Hint: uncovered paths look like validation/branches; add tests for boundary inputs.";
    }

    @Override
    public String visit(ProbeMetadata.BranchProbe p) {
        return "Hint: " + hintForCondition(p.conditionText(), p.direction());
    }

    @Override
    public String visit(ProbeMetadata.ReturnProbe p) {
        return "Hint: add a test that returns from this path.";
    }

    @Override
    public String visit(ProbeMetadata.ThrowProbe p) {
        return "Hint: add a test that triggers this exception path.";
    }

    @Override
    public String visit(ProbeMetadata.SegmentProbe p) {
        return "Hint: add test that executes this code block.";
    }

    // -----------------------------------------------------------------------
    // Condition heuristics
    // -----------------------------------------------------------------------

    private String hintForCondition(String conditionText, ProbeMetadata.BranchDirection direction) {
        if (conditionText.contains("!= null")) {
            return direction == ProbeMetadata.BranchDirection.TRUE
                    ? "add test with a non-null value."
                    : "add test with null argument (expect exception or fallback).";
        }
        if (conditionText.contains("== null")) {
            return direction == ProbeMetadata.BranchDirection.TRUE
                    ? "add test with null argument (expect exception or fallback)."
                    : "add test with a non-null value.";
        }
        if (conditionText.contains("== 0")) {
            return "add test with zero value.";
        }
        if (conditionText.contains("isEmpty")) {
            return "add test with an empty collection or string.";
        }
        return "add test with boundary values (e.g. equal, just below, just above).";
    }
}

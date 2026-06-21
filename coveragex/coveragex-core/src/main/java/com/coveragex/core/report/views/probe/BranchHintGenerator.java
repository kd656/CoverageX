package com.coveragex.core.report.views.probe;

public final class BranchHintGenerator {

    private BranchHintGenerator() {}

    public static String hint(String condText, boolean isTrue, boolean trueHit, boolean falseHit) {
        if (trueHit && falseHit) return "Both directions are covered. No action needed.";
        boolean missed = (isTrue && !trueHit) || (!isTrue && !falseHit);
        if (!missed) return "";
        if (condText != null && condText.contains("!= null")) {
            return isTrue ? "Add a test with a non-null argument."
                          : "Pass a null argument in a test to exercise this path.";
        }
        if (condText != null && condText.contains("== null")) {
            return isTrue ? "Pass a null argument in a test to exercise this path."
                          : "Add a test with a non-null argument.";
        }
        if (condText != null && (condText.contains("isEmpty()") || condText.contains("size() == 0"))) {
            return "Add a test with a non-empty collection.";
        }
        return isTrue ? "Add a test that makes this condition true."
                      : "Add a test that makes this condition false.";
    }
}

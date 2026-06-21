package com.coveragex.compat.contract;

import com.coveragex.api.data.ClassCoverage;
import com.coveragex.api.data.InvocationRecord;
import com.coveragex.api.data.MethodHit;
import org.assertj.core.api.SoftAssertions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Verifies the captured arguments of method-entry invocations.
 *
 * <p>For each method name, the contract holds the args lists that the
 * matrix expects to see in {@link InvocationRecord#args()} entries.
 * Default semantics are <strong>exact match</strong> per {@code
 * documentation/coveragex-runtime-capture-contracts-plan.md} §10.1 —
 * the captured set must equal the expected set with no extras and no
 * missing entries. Fixtures that genuinely cannot pin the exact set
 * opt into looseness per method via the {@code .containsAtLeast(...)}
 * builder method.</p>
 */
public record ArgsContract(
        Map<String, List<List<String>>> expectedArgsByMethod,
        Map<String, MatchMode> modeByMethod) {

    public ArgsContract {
        expectedArgsByMethod = Map.copyOf(expectedArgsByMethod);
        modeByMethod = Map.copyOf(modeByMethod);
    }

    public enum MatchMode { EXACT, CONTAINS_AT_LEAST }

    public static Builder builder() {
        return new Builder();
    }

    public void verify(ClassCoverage classCoverage) {
        Map<String, List<List<String>>> capturedByMethod = capturedArgsByMethod(classCoverage);
        SoftAssertions softly = new SoftAssertions();
        for (var entry : expectedArgsByMethod.entrySet()) {
            String methodName = entry.getKey();
            List<List<String>> expected = entry.getValue();
            List<List<String>> captured = capturedByMethod.getOrDefault(methodName, List.of());
            MatchMode mode = modeByMethod.getOrDefault(methodName, MatchMode.EXACT);
            if (mode == MatchMode.EXACT) {
                softly.assertThat(captured)
                        .as("args for method '%s' (exact match)", methodName)
                        .containsExactlyInAnyOrderElementsOf(expected);
            } else {
                softly.assertThat(captured)
                        .as("args for method '%s' (contains-at-least)", methodName)
                        .containsAll(expected);
            }
        }
        softly.assertAll();
    }

    private static Map<String, List<List<String>>> capturedArgsByMethod(ClassCoverage cc) {
        Map<String, List<List<String>>> byMethod = new HashMap<>();
        for (MethodHit hit : cc.methodHits().values()) {
            List<List<String>> argTuples = new java.util.ArrayList<>(hit.invocations().size());
            for (InvocationRecord ir : hit.invocations()) {
                argTuples.add(ir.args());
            }
            byMethod.computeIfAbsent(hit.methodName(), k -> new java.util.ArrayList<>()).addAll(argTuples);
        }
        return byMethod;
    }

    public static final class Builder {
        private final Map<String, List<List<String>>> args = new HashMap<>();
        private final Map<String, MatchMode> modes = new HashMap<>();

        /** Pins exact args for a method. Captured args must equal the expected set. */
        public Builder method(String methodName, List<List<String>> argTuples) {
            args.put(methodName, List.copyOf(argTuples));
            modes.put(methodName, MatchMode.EXACT);
            return this;
        }

        /**
         * Pins a subset of args for a method. Captured args must include each
         * expected tuple but may have extras. Use when JDK desugaring or
         * non-determinism makes exact match unworkable.
         */
        public Builder containsAtLeast(String methodName, List<List<String>> argTuples) {
            args.put(methodName, List.copyOf(argTuples));
            modes.put(methodName, MatchMode.CONTAINS_AT_LEAST);
            return this;
        }

        public ArgsContract build() {
            return new ArgsContract(args, modes);
        }
    }
}

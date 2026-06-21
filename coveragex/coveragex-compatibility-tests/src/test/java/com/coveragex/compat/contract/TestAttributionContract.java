package com.coveragex.compat.contract;

import com.coveragex.api.data.AttributedInvocation;
import com.coveragex.api.data.ClassCoverage;
import com.coveragex.api.data.ClassTestCoverage;
import com.coveragex.api.data.MethodHit;
import org.assertj.core.api.SoftAssertions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Verifies which test contexts each method's hits were attributed to.
 *
 * <p>The runner drives the fixture by iterating
 * {@link com.coveragex.compat.spec.InvocationStep} entries from the spec,
 * opening a {@code contextRegistry().scope(...)} per step. The collector
 * tags every hit with the active context. This contract asserts the
 * resulting per-method test-name set matches expectations.</p>
 */
public record TestAttributionContract(
        Map<String, Set<String>> expectedTestsByMethod) {

    public TestAttributionContract {
        Map<String, Set<String>> copy = new HashMap<>();
        expectedTestsByMethod.forEach((k, v) -> copy.put(k, Set.copyOf(v)));
        expectedTestsByMethod = Map.copyOf(copy);
    }

    public static Builder builder() {
        return new Builder();
    }

    public void verify(ClassCoverage classCoverage) {
        // Resolve method-name -> set of method-entry probe IDs from methodHits.
        Map<String, Set<Integer>> probeIdsByMethod = new HashMap<>();
        for (var entry : classCoverage.methodHits().entrySet()) {
            int probeId = entry.getKey();
            MethodHit hit = entry.getValue();
            probeIdsByMethod.computeIfAbsent(hit.methodName(), k -> new HashSet<>()).add(probeId);
        }

        ClassTestCoverage tracking = classCoverage.testAttribution();
        Map<Integer, List<AttributedInvocation>> attribution = tracking.probeInvocations();

        SoftAssertions softly = new SoftAssertions();
        for (var entry : expectedTestsByMethod.entrySet()) {
            String methodName = entry.getKey();
            Set<String> expectedTests = entry.getValue();
            Set<Integer> probeIds = probeIdsByMethod.getOrDefault(methodName, Set.of());

            Set<String> actualTests = new HashSet<>();
            for (int probeId : probeIds) {
                List<AttributedInvocation> ais = attribution.getOrDefault(probeId, List.of());
                for (AttributedInvocation ai : ais) {
                    actualTests.addAll(ai.testMethods());
                }
            }

            softly.assertThat(actualTests)
                    .as("test contexts attributed to method '%s'", methodName)
                    .containsExactlyInAnyOrderElementsOf(expectedTests);
        }
        softly.assertAll();
    }

    public static final class Builder {
        private final Map<String, Set<String>> testsByMethod = new HashMap<>();

        public Builder method(String methodName, String... testContexts) {
            testsByMethod.put(methodName, Set.of(testContexts));
            return this;
        }

        public TestAttributionContract build() {
            return new TestAttributionContract(testsByMethod);
        }
    }
}

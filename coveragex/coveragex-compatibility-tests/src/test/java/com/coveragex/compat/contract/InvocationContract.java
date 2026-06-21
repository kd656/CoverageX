package com.coveragex.compat.contract;

import com.coveragex.api.data.ClassCoverage;
import com.coveragex.api.data.InvocationRecord;
import com.coveragex.api.data.MethodHit;
import org.assertj.core.api.SoftAssertions;

import java.util.HashMap;
import java.util.Map;

/**
 * Verifies how many times each method was actually invoked.
 *
 * <p>Whereas {@link HitsContract} answers "did each probe light up at
 * least once?", this contract sums the {@link InvocationRecord#count()}
 * fields across every recorded invocation of a method-entry probe. The
 * dimension where they diverge: a recursive method has one probe that
 * may light up dozens of times, and only this contract surfaces that.</p>
 */
public record InvocationContract(
        Map<String, Integer> minInvocationsByMethod) {

    public InvocationContract {
        minInvocationsByMethod = Map.copyOf(minInvocationsByMethod);
    }

    public static Builder builder() {
        return new Builder();
    }

    public void verify(ClassCoverage classCoverage) {
        Map<String, Integer> totalsByMethod = new HashMap<>();
        for (MethodHit hit : classCoverage.methodHits().values()) {
            int total = hit.invocations().stream().mapToInt(InvocationRecord::count).sum();
            totalsByMethod.merge(hit.methodName(), total, Integer::sum);
        }
        SoftAssertions softly = new SoftAssertions();
        for (var entry : minInvocationsByMethod.entrySet()) {
            String methodName = entry.getKey();
            int expectedMin = entry.getValue();
            int actual = totalsByMethod.getOrDefault(methodName, 0);
            softly.assertThat(actual)
                    .as("total invocation count for method '%s'", methodName)
                    .isGreaterThanOrEqualTo(expectedMin);
        }
        softly.assertAll();
    }

    public static final class Builder {
        private final Map<String, Integer> minByMethod = new HashMap<>();

        public Builder method(String methodName, int minInvocations) {
            minByMethod.put(methodName, minInvocations);
            return this;
        }

        public InvocationContract build() {
            return new InvocationContract(minByMethod);
        }
    }
}

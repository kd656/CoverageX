package com.coveragex.core.fixtures.lambdas;

import java.util.List;
import java.util.stream.Collectors;

public class LambdaWithCondition {
    public List<Integer> filterPositive(List<Integer> numbers) {
        return numbers.stream()
            .filter(n -> n > 0)
            .collect(Collectors.toList());
    }
}

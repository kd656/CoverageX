package com.coveragex.core.fixtures.lambdas;

import java.util.List;
import java.util.stream.Collectors;

public class MethodReference {
    public List<String> convertToStrings(List<Integer> numbers) {
        return numbers.stream()
            .map(String::valueOf)
            .collect(Collectors.toList());
    }

    public List<String> toUpperCase(List<String> items) {
        return items.stream()
            .map(String::toUpperCase)
            .collect(Collectors.toList());
    }
}

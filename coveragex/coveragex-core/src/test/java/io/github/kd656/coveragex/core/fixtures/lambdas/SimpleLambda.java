package io.github.kd656.coveragex.core.fixtures.lambdas;

import java.util.List;
import java.util.stream.Collectors;

public class SimpleLambda {
    public List<String> upperCaseAll(List<String> items) {
        return items.stream()
            .map(s -> s.toUpperCase())
            .collect(Collectors.toList());
    }
}

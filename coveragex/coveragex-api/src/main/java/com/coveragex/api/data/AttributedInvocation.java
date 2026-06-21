package com.coveragex.api.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One unique args combination and the test methods that produced it.
 */
public record AttributedInvocation(List<String> args, List<String> testMethods) {
    public AttributedInvocation {
        args = Collections.unmodifiableList(new ArrayList<>(args));
        testMethods = List.copyOf(testMethods);
    }
}

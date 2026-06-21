package com.coveragex.core.report.views.html;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Join of AttributedInvocation (args + tests) and InvocationRecord (count) for template rendering.
 * args may contain null elements (null arg != string "null").
 */
public record InvocationView(List<String> args, List<String> testMethods, int count) {
    public InvocationView {
        args        = Collections.unmodifiableList(new ArrayList<>(args));
        testMethods = List.copyOf(testMethods);
    }
}

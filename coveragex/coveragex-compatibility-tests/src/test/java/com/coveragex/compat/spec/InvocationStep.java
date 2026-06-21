package com.coveragex.compat.spec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One step in a fixture's invocation plan.
 *
 * <p>Drives the runner: instead of calling {@code fixture.execute()} blindly,
 * the runner iterates the spec's {@code invocationPlan()}, opening a
 * {@code contextRegistry().scope(testContext)} around each step and
 * reflectively invoking {@code fixture.methodName(args...)}. This lets
 * {@link com.coveragex.compat.contract.TestAttributionContract} assert
 * "method X was hit under test context A but not test context B" without
 * the fixture itself having to grow per-test entry methods.</p>
 *
 * <p>Empty plan (default) means: call {@code execute()} once under the
 * default no-context behaviour.</p>
 */
public record InvocationStep(String methodName, List<Object> args, String testContext) {

    public InvocationStep {
        // unmodifiableList so plan args can contain null (e.g. NullCheck#orDefault(null))
        args = Collections.unmodifiableList(new ArrayList<>(args));
    }

    public static InvocationStep of(String methodName, List<Object> args, String testContext) {
        return new InvocationStep(methodName, args, testContext);
    }
}

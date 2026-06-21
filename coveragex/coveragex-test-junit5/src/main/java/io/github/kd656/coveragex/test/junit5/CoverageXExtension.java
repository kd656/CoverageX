package io.github.kd656.coveragex.test.junit5;

import io.github.kd656.coveragex.api.context.ProbeExecutionContext;
import io.github.kd656.coveragex.api.context.StandardContextKeys;
import io.github.kd656.coveragex.test.api.ProbeExecutionContexts;
import io.github.kd656.coveragex.test.api.TestContextHolder;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class CoverageXExtension implements BeforeEachCallback, AfterEachCallback {

    private static final JUnit5ProbeExecutionContextProvider PROVIDER =
            new JUnit5ProbeExecutionContextProvider();

    @Override
    public void beforeEach(ExtensionContext ctx) {
        String id = ctx.getRequiredTestClass().getName()
                + "#" + ctx.getRequiredTestMethod().getName();

        ProbeExecutionContext context = ProbeExecutionContexts.builder(id)
            .put(StandardContextKeys.TEST_CLASS, ctx.getRequiredTestClass().getName())
            .put(StandardContextKeys.TEST_METHOD, ctx.getRequiredTestMethod().getName())
            .put(StandardContextKeys.TEST_DISPLAY_NAME, ctx.getDisplayName())
            .build();

        TestContextHolder.set(context);
    }

    @Override
    public void afterEach(ExtensionContext ctx) {
        TestContextHolder.clear();
    }
}

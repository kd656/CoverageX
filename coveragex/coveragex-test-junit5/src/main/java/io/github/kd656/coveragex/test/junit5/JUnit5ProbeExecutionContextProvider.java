package io.github.kd656.coveragex.test.junit5;

import io.github.kd656.coveragex.api.context.ProbeExecutionContext;
import io.github.kd656.coveragex.test.api.ProbeExecutionContextProvider;
import io.github.kd656.coveragex.test.api.TestContextHolder;
import java.util.Optional;

public class JUnit5ProbeExecutionContextProvider implements ProbeExecutionContextProvider {

    @Override
    public Optional<ProbeExecutionContext> currentContext() {
        return TestContextHolder.get();
    }
}

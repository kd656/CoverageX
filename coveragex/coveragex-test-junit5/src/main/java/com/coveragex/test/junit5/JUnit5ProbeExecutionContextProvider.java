package com.coveragex.test.junit5;

import com.coveragex.api.context.ProbeExecutionContext;
import com.coveragex.test.api.ProbeExecutionContextProvider;
import com.coveragex.test.api.TestContextHolder;
import java.util.Optional;

public class JUnit5ProbeExecutionContextProvider implements ProbeExecutionContextProvider {

    @Override
    public Optional<ProbeExecutionContext> currentContext() {
        return TestContextHolder.get();
    }
}

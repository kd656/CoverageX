package com.coveragex.test.api;

import com.coveragex.api.context.ProbeExecutionContext;
import java.util.Optional;

public interface ProbeExecutionContextProvider {

    Optional<ProbeExecutionContext> currentContext();

    ProbeExecutionContextProvider NOOP = Optional::empty;
}

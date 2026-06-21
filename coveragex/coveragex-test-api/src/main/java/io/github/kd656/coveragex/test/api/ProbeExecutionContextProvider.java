package io.github.kd656.coveragex.test.api;

import io.github.kd656.coveragex.api.context.ProbeExecutionContext;
import java.util.Optional;

public interface ProbeExecutionContextProvider {

    Optional<ProbeExecutionContext> currentContext();

    ProbeExecutionContextProvider NOOP = Optional::empty;
}

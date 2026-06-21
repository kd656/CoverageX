package io.github.kd656.coveragex.api.context;

import java.util.Optional;
import java.util.Set;

public interface ProbeExecutionContext {

    /** Stable deduplication key. Typically "com.example.MyTest#testMethod". */
    String id();

    <T> Optional<T> get(ContextKey<T> key);

    Set<ContextKey<?>> keys();
}

package io.github.kd656.coveragex.core.instrument;

public interface ProbeInjector<T> {

    byte[] injectProbes(String className, T model);
}

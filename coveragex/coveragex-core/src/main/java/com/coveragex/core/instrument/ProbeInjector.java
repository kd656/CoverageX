package com.coveragex.core.instrument;

public interface ProbeInjector<T> {

    byte[] injectProbes(String className, T model);
}

package io.github.kd656.coveragex.core.analysis.source;

import java.io.IOException;
import java.nio.file.Path;

public interface CoverageContextLoader<T> {

    T load(Path path) throws IOException;

    void save(Path path, T model) throws IOException;
}

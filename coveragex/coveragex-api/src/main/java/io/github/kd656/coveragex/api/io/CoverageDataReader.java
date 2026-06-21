package io.github.kd656.coveragex.api.io;

import java.io.IOException;
import java.nio.file.Path;

public interface CoverageDataReader<T> {
    T read(Path path) throws IOException;
}

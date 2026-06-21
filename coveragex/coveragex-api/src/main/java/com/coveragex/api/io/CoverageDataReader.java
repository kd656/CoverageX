package com.coveragex.api.io;

import java.io.IOException;
import java.nio.file.Path;

public interface CoverageDataReader<T> {
    T read(Path path) throws IOException;
}

package com.coveragex.api.io;

import java.io.IOException;
import java.nio.file.Path;

public interface CoverageDataWriter<T> {
    void write(Path outputPath, T data) throws IOException;
}

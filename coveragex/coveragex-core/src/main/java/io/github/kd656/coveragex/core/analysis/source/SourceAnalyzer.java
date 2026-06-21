package io.github.kd656.coveragex.core.analysis.source;

import java.io.IOException;
import java.nio.file.Path;

public interface SourceAnalyzer {

    void scan(Path root) throws IOException;
}

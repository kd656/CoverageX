package io.github.kd656.coveragex.core.report;

import java.nio.file.Path;

public record ReportContext(Path reportOutputDir, Path sourceDirectory) {}

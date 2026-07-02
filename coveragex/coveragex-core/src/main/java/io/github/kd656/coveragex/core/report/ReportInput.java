package io.github.kd656.coveragex.core.report;

import io.github.kd656.coveragex.api.data.ExecutionData;

import java.nio.file.Path;

/**
 * Reusable input boundary between the build-tool plugin and the reporting stack.
 *
 * <p>Single-module reporting passes a one-element list; multi-module aggregation
 * passes one entry per module. The {@code scopeId} is sanitized to be safe for
 * filesystem paths and DOM ids.</p>
 */
public record ReportInput(
        String scopeId,
        String displayName,
        Path sourceDirectory,
        ExecutionData executionData
) {}

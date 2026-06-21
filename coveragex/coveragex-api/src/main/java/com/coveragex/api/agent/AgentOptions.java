package com.coveragex.api.agent;

import java.util.List;

/**
 * Configuration options passed to the CoverageX agent at JVM startup.
 * <p>{@code null} list arguments are normalised to empty lists by the compact constructor.</p>
 */
public record AgentOptions(
        String destFile,
        String mapFile,
        List<String> includes,
        List<String> excludes
) {
    public AgentOptions {
        includes = includes == null ? List.of() : List.copyOf(includes);
        excludes = excludes == null ? List.of() : List.copyOf(excludes);
    }
}

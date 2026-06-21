package com.coveragex.api.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builds the semicolon-delimited argument string passed to the CoverageX agent via {@code -javaagent}.
 * <p>List-valued options ({@code include}, {@code exclude}) are emitted as repeated key=value pairs.</p>
 */
public final class AgentArgumentBuilder {

    public String build(AgentOptions options) {
        Objects.requireNonNull(options, "options must not be null");

        List<String> entries = new ArrayList<>();
        addIfPresent(entries, "destfile", options.destFile());
        addIfPresent(entries, "mapfile", options.mapFile());
        addRepeated(entries, "include", options.includes());
        addRepeated(entries, "exclude", options.excludes());
        return String.join(";", entries);
    }

    private void addIfPresent(List<String> entries, String key, String value) {
        if (value != null && !value.isBlank()) {
            entries.add(key + "=" + value);
        }
    }

    private void addRepeated(List<String> entries, String key, List<String> values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                entries.add(key + "=" + value);
            }
        }
    }
}

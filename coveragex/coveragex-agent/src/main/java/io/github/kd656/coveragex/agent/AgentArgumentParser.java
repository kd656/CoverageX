package io.github.kd656.coveragex.agent;

import io.github.kd656.coveragex.api.agent.AgentOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses the semicolon-delimited agent argument string produced by {@link io.github.kd656.coveragex.api.agent.AgentArgumentBuilder}.
 * <p>Unknown keys are silently ignored for forward compatibility.</p>
 */
final class AgentArgumentParser {

    AgentOptions parse(String agentArgs) {
        if (agentArgs == null || agentArgs.isBlank()) {
            return new AgentOptions(null, null, List.of(), List.of());
        }
        return parseSemicolonArguments(agentArgs);
    }

    private AgentOptions parseSemicolonArguments(String agentArgs) {
        String destFile = null;
        String mapFile = null;
        List<String> includes = new ArrayList<>();
        List<String> excludes = new ArrayList<>();

        for (String arg : agentArgs.split(";")) {
            String[] parts = arg.split("=", 2);
            if (parts.length != 2) {
                continue;
            }

            String key = parts[0].trim();
            String value = parts[1].trim();
            if (value.isEmpty()) {
                continue;
            }

            switch (key) {
                case "destfile" -> destFile = value;
                case "mapfile" -> mapFile = value;
                case "include", "includes" -> includes.add(value);
                case "exclude", "excludes" -> excludes.add(value);
                default -> {
                    // Unknown options are ignored for forward compatibility.
                }
            }
        }

        return new AgentOptions(destFile, mapFile, includes, excludes);
    }
}

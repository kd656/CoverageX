package com.coveragex.api.agent;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AgentArgumentBuilderTest {

    @Test
    void buildsSemicolonSeparatedAgentArguments() {
        AgentOptions options = new AgentOptions(
                "target/coveragex.exec",
                "target/test-classes/coveragex/coveragex.map.json",
                List.of("org.example.**", "com.acme.*"),
                List.of("**.*Test")
        );

        String args = new AgentArgumentBuilder().build(options);

        assertThat(args).isEqualTo(
                "destfile=target/coveragex.exec;"
                        + "mapfile=target/test-classes/coveragex/coveragex.map.json;"
                        + "include=org.example.**;"
                        + "include=com.acme.*;"
                        + "exclude=**.*Test"
        );
    }

    @Test
    void skipsBlankValues() {
        AgentOptions options = new AgentOptions("", null, List.of("org.example.**", " "), List.of());

        String args = new AgentArgumentBuilder().build(options);

        assertThat(args).isEqualTo("include=org.example.**");
    }
}

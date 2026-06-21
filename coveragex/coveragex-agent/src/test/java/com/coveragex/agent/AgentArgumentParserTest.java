package com.coveragex.agent;

import com.coveragex.api.agent.AgentOptions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentArgumentParserTest {

    @Test
    void parsesSemicolonSeparatedArgumentsWithRepeatedPatterns() {
        AgentOptions options = new AgentArgumentParser().parse(
                "destfile=build/coveragex/coveragex.exec;"
                        + "mapfile=build/classes/java/test/coveragex/coveragex.map.json;"
                        + "include=org.example.**;"
                        + "include=com.acme.*;"
                        + "exclude=**.*Test"
        );

        assertThat(options.destFile()).isEqualTo("build/coveragex/coveragex.exec");
        assertThat(options.mapFile()).isEqualTo("build/classes/java/test/coveragex/coveragex.map.json");
        assertThat(options.includes()).containsExactly("org.example.**", "com.acme.*");
        assertThat(options.excludes()).containsExactly("**.*Test");
    }

    @Test
    void returnsDefaultOptionsForNullArgs() {
        AgentOptions options = new AgentArgumentParser().parse(null);
        assertThat(options.destFile()).isNull();
        assertThat(options.mapFile()).isNull();
        assertThat(options.includes()).isEmpty();
        assertThat(options.excludes()).isEmpty();
    }

    @Test
    void returnsDefaultOptionsForBlankArgs() {
        AgentOptions options = new AgentArgumentParser().parse("   ");
        assertThat(options.destFile()).isNull();
        assertThat(options.includes()).isEmpty();
    }

    @Test
    void ignoresUnknownKeys() {
        AgentOptions options = new AgentArgumentParser().parse("destfile=out.exec;unknownkey=value");
        assertThat(options.destFile()).isEqualTo("out.exec");
    }

    @Test
    void ignoresMalformedSegments() {
        AgentOptions options = new AgentArgumentParser().parse("=value;key=;keyonly;destfile=ok.exec");
        assertThat(options.destFile()).isEqualTo("ok.exec");
    }

    @Test
    void trimsKeyAndValueWhitespace() {
        AgentOptions options = new AgentArgumentParser().parse(" destfile = out.exec ; include = com.example.** ");
        assertThat(options.destFile()).isEqualTo("out.exec");
        assertThat(options.includes()).containsExactly("com.example.**");
    }

    @Test
    void duplicateDestfileUsesLastValue() {
        AgentOptions options = new AgentArgumentParser().parse("destfile=first.exec;destfile=second.exec");
        assertThat(options.destFile()).isEqualTo("second.exec");
    }

    @Test
    void emptyIncludeAndExcludeValuesAreIgnored() {
        AgentOptions options = new AgentArgumentParser().parse("include=;exclude=;destfile=ok.exec");
        assertThat(options.includes()).isEmpty();
        assertThat(options.excludes()).isEmpty();
        assertThat(options.destFile()).isEqualTo("ok.exec");
    }

    @Test
    void delimiterCharactersInsideValueArePreserved() {
        AgentOptions options = new AgentArgumentParser().parse("destfile=path/to/my.exec");
        assertThat(options.destFile()).isEqualTo("path/to/my.exec");
    }
}

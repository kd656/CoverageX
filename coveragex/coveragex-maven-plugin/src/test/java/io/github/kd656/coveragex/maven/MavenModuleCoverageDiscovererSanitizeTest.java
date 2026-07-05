package io.github.kd656.coveragex.maven;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sanitize is the piece the discoverer applies to raw artifact ids before they
 * are used as scope ids (filesystem paths, DOM ids). This test locks the rules
 * without needing a real MavenSession.
 */
class MavenModuleCoverageDiscovererSanitizeTest {

    @Test
    void alphanumericIdIsUnchanged() {
        assertThat(MavenModuleCoverageDiscoverer.sanitize("dto")).isEqualTo("dto");
        assertThat(MavenModuleCoverageDiscoverer.sanitize("service-a")).isEqualTo("service-a");
    }

    @Test
    void replacesUnsafeCharactersWithDash() {
        assertThat(MavenModuleCoverageDiscoverer.sanitize("com/example")).isEqualTo("com-example");
        assertThat(MavenModuleCoverageDiscoverer.sanitize("weird chars!")).isEqualTo("weird-chars");
    }

    @Test
    void collapsesConsecutiveDashes() {
        assertThat(MavenModuleCoverageDiscoverer.sanitize("a--b---c")).isEqualTo("a-b-c");
    }

    @Test
    void trimsLeadingAndTrailingDashes() {
        assertThat(MavenModuleCoverageDiscoverer.sanitize("-name-")).isEqualTo("name");
        assertThat(MavenModuleCoverageDiscoverer.sanitize("--triple--")).isEqualTo("triple");
    }

    @Test
    void allowsDotUnderscoreAndDash() {
        assertThat(MavenModuleCoverageDiscoverer.sanitize("com.acme_dto-1"))
            .isEqualTo("com.acme_dto-1");
    }

    @Test
    void fallsBackToDefaultForBlankOrDashOnly() {
        assertThat(MavenModuleCoverageDiscoverer.sanitize("")).isEqualTo("module");
        assertThat(MavenModuleCoverageDiscoverer.sanitize(null)).isEqualTo("module");
        assertThat(MavenModuleCoverageDiscoverer.sanitize("---")).isEqualTo("module");
    }
}

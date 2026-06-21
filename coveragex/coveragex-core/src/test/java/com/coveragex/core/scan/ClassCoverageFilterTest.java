package com.coveragex.core.scan;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClassCoverageFilterTest {

    @Test
    void defaultPolicyIncludesProductionClassesAndExcludesSystemPackages() {
        ClassCoverageFilter filter = new ClassCoverageFilter(null, null);

        assertThat(filter.shouldInclude("org/example/Main", Path.of("target/classes/org/example/Main.class"),
                ClassOrigin.PRODUCTION_OUTPUT)).isTrue();
        assertThat(filter.shouldInclude("java/lang/String", Path.of("target/classes/java/lang/String.class"),
                ClassOrigin.PRODUCTION_OUTPUT)).isFalse();
    }

    @Test
    void explicitIncludePatternsRespectPackageDepthAndExcludesWin() {
        ClassCoverageFilter filter = new ClassCoverageFilter(
                List.of("org.example.**"),
                List.of("org.example.internal.**"));

        assertThat(filter.shouldInclude("org/example/Main", null, ClassOrigin.PRODUCTION_OUTPUT)).isTrue();
        assertThat(filter.shouldInclude("org/example/xxx/Other", null, ClassOrigin.PRODUCTION_OUTPUT)).isTrue();
        assertThat(filter.shouldInclude("org/Foo", null, ClassOrigin.PRODUCTION_OUTPUT)).isFalse();
        assertThat(filter.shouldInclude("org/example/internal/Hidden", null, ClassOrigin.PRODUCTION_OUTPUT)).isFalse();
    }

    @Test
    void testOutputOriginIsAlwaysExcluded() {
        ClassCoverageFilter filter = new ClassCoverageFilter(List.of("org.example.**"), null);

        assertThat(filter.shouldInclude("org/example/Main", Path.of("target/test-classes/org/example/Main.class"),
                ClassOrigin.TEST_OUTPUT)).isFalse();
    }
}

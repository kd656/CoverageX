package io.github.kd656.coveragex.compat.spec;

import io.github.kd656.coveragex.compat.spec.fixture.UnnamedPatternInSwitchSpec;
import org.junit.jupiter.api.Test;

import java.util.ServiceLoader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Compiled only under profiles that include the JDK 22 forward module
 * ({@code -Pfixtures-jdk22} or any cumulative-superset profile such as
 * {@code -Pfixtures-jdk25}). Verifies that {@link Jdk22FixtureCatalog} is
 * actually discovered by {@link ServiceLoader} when the JAR is on the
 * test classpath.
 */
class Jdk22CatalogContributorTest {

    @Test
    void contributorIsDiscoverableAndContributesJdk22Specs() {
        Jdk22FixtureCatalog contributor = ServiceLoader.load(FixtureCatalogContributor.class).stream()
                .map(ServiceLoader.Provider::get)
                .filter(Jdk22FixtureCatalog.class::isInstance)
                .map(Jdk22FixtureCatalog.class::cast)
                .findFirst()
                .orElse(null);

        assertThat(contributor)
                .as("Jdk22FixtureCatalog should be discovered by ServiceLoader under -Pfixtures-jdk22 or any cumulative-superset profile")
                .isNotNull();
        assertThat(contributor.specs())
                .as("Jdk22FixtureCatalog should contribute JDK 22 fixture specs")
                .hasSize(1)
                .allSatisfy(spec -> assertThat(spec).isInstanceOf(UnnamedPatternInSwitchSpec.class));
    }
}

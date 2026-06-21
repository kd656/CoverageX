package com.coveragex.compat.spec;

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
    void contributorIsDiscoverable() {
        boolean found = false;
        for (FixtureCatalogContributor c : ServiceLoader.load(FixtureCatalogContributor.class)) {
            if (c instanceof Jdk22FixtureCatalog) {
                found = true;
                break;
            }
        }
        assertThat(found)
                .as("Jdk22FixtureCatalog should be discovered by ServiceLoader under -Pfixtures-jdk22 or any cumulative-superset profile")
                .isTrue();
    }
}

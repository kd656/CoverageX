package com.coveragex.compat.spec;

import org.junit.jupiter.api.Test;

import java.util.ServiceLoader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Compiled only under {@code -Pfixtures-jdk25} via the profile-gated
 * {@code src/test/java-jdk25} source root. Verifies that the
 * {@link Jdk25FixtureCatalog} contributor is actually discovered by
 * {@link ServiceLoader} when the forward profile is active.
 *
 * <p>Without this test we could ship an empty contributor that ServiceLoader
 * never finds (broken {@code META-INF/services} entry, missing test-resource
 * root, classpath ordering bug) and the matrix wouldn't tell us.</p>
 */
class Jdk25CatalogContributorTest {

    @Test
    void contributorIsDiscoverable() {
        boolean found = false;
        for (FixtureCatalogContributor c : ServiceLoader.load(FixtureCatalogContributor.class)) {
            if (c instanceof Jdk25FixtureCatalog) {
                found = true;
                break;
            }
        }
        assertThat(found)
                .as("Jdk25FixtureCatalog should be discovered by ServiceLoader under -Pfixtures-jdk25")
                .isTrue();
    }
}

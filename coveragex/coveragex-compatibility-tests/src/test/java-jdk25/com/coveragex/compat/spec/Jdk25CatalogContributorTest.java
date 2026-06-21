package com.coveragex.compat.spec;

import com.coveragex.compat.spec.fixture.FlexibleCtorBodySpec;
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
    void contributorIsDiscoverableAndContributesJdk25Specs() {
        Jdk25FixtureCatalog contributor = ServiceLoader.load(FixtureCatalogContributor.class).stream()
                .map(ServiceLoader.Provider::get)
                .filter(Jdk25FixtureCatalog.class::isInstance)
                .map(Jdk25FixtureCatalog.class::cast)
                .findFirst()
                .orElse(null);

        assertThat(contributor)
                .as("Jdk25FixtureCatalog should be discovered by ServiceLoader under -Pfixtures-jdk25")
                .isNotNull();
        assertThat(contributor.specs())
                .as("Jdk25FixtureCatalog should contribute JDK 25 fixture specs")
                .hasSize(1)
                .allSatisfy(spec -> assertThat(spec).isInstanceOf(FlexibleCtorBodySpec.class));
    }
}

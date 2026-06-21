package io.github.kd656.coveragex.compat.spec;

import io.github.kd656.coveragex.compat.spec.fixture.FlexibleCtorBodySpec;

import java.util.List;

/**
 * {@link FixtureCatalogContributor} for {@code coveragex-test-fixtures-jdk25}.
 *
 * <p>Compiled only under {@code -Pfixtures-jdk25} via the profile-gated
 * {@code src/test/java-jdk25} source root. The matching {@code META-INF/services}
 * entry under {@code src/test/resources-jdk25} makes
 * {@link java.util.ServiceLoader} discover this class when (and only when)
 * the profile is active.</p>
 *
 * <p>PR B shipped this empty. PR D adds the first real forward-fixture spec
 * ({@link FlexibleCtorBodySpec} — JDK 25 flexible constructor bodies).</p>
 */
public final class Jdk25FixtureCatalog implements FixtureCatalogContributor {

    @Override
    public List<FixtureContractSpec> specs() {
        return List.of(
                new FlexibleCtorBodySpec()
        );
    }
}

package com.coveragex.compat.spec;

import com.coveragex.compat.spec.fixture.UnnamedPatternInSwitchSpec;

import java.util.List;

/**
 * {@link FixtureCatalogContributor} for {@code coveragex-test-fixtures-jdk22}.
 *
 * <p>Compiled only when a profile that includes JDK 22-era fixtures is active
 * ({@code -Pfixtures-jdk22} or any later cumulative profile such as
 * {@code -Pfixtures-jdk25}). The matching {@code META-INF/services} entry
 * under {@code src/test/resources-jdk22} makes {@link java.util.ServiceLoader}
 * discover this class when the JAR is on the classpath.</p>
 */
public final class Jdk22FixtureCatalog implements FixtureCatalogContributor {

    @Override
    public List<FixtureContractSpec> specs() {
        return List.of(
                new UnnamedPatternInSwitchSpec()
        );
    }
}

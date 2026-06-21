package io.github.kd656.coveragex.compat.spec;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the composed catalog's invariants regardless of which profile
 * is active:
 *
 * <ul>
 *   <li>{@link FixtureCatalog#all()} contains every base spec.</li>
 *   <li>All registered FQNs are unique (no two specs claim the same fixture).</li>
 *   <li>No fixture's class file is shadowed by multiple JARs on the classpath
 *       — catches the failure mode where a forward module accidentally
 *       ships a base-fixture FQN and the classloader silently serves the
 *       first match.</li>
 *   <li>Under the default profile only, the composed catalog equals
 *       {@link BaseFixtureCatalog#specs()} (no forward contributors
 *       discovered).</li>
 * </ul>
 */
class FixtureCatalogTest {

    @Test
    void allContainsEveryBaseSpec() {
        List<FixtureContractSpec> base = BaseFixtureCatalog.specs();
        List<FixtureContractSpec> all = FixtureCatalog.all();

        assertThat(all).containsAll(base);
    }

    @Test
    void noDuplicateFixtureFqns() {
        List<FixtureContractSpec> all = FixtureCatalog.all();

        Set<String> seen = new HashSet<>();
        for (FixtureContractSpec spec : all) {
            assertThat(seen.add(spec.fqn()))
                    .as("duplicate fixture FQN registered: %s", spec.fqn())
                    .isTrue();
        }
    }

    /**
     * §9.1 smoke test. {@link ClassLoader#getResources(String)} returns every
     * URL on the classpath for the given resource name. A fixture's
     * {@code .class} file appearing on multiple URLs means two fixture
     * artifacts shipped the same FQN — the JVM would load only the first
     * one found, silently masking the duplicate.
     */
    @Test
    void noFixtureClassResourceIsShadowed() throws Exception {
        for (FixtureContractSpec spec : FixtureCatalog.all()) {
            String resource = spec.fqn().replace('.', '/') + ".class";
            List<URL> urls = Collections.list(
                    getClass().getClassLoader().getResources(resource));
            assertThat(urls)
                    .as("classpath contains multiple copies of fixture %s", spec.fqn())
                    .hasSize(1);
        }
    }

    @Test
    void defaultProfileHasNoForwardContributors() {
        // Under -Pfixtures-jdk<N> forward contributors join the catalog by design.
        // Skip when the active row is not the base.
        Assumptions.assumeTrue(
                "21".equals(System.getProperty("fixture.jdk", "21")),
                "Skipping: forward fixture profile is active");
        assertThat(FixtureCatalog.all()).hasSameSizeAs(BaseFixtureCatalog.specs());
    }
}

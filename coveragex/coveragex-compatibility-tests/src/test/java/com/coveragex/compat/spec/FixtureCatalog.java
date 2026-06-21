package com.coveragex.compat.spec;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Authoritative composed list of every fixture the matrix exercises.
 *
 * <p>The catalog is built once at class-load time from two sources:</p>
 *
 * <ul>
 *   <li>{@link BaseFixtureCatalog#specs()} — the floor-JDK fixtures
 *       (every fixture whose syntax compiles on the current matrix floor).</li>
 *   <li>Every {@link FixtureCatalogContributor} discovered on the test
 *       classpath via {@link ServiceLoader}. Forward-module fixtures
 *       (per-JDK sources whose syntax the floor cannot parse) join the
 *       catalog through this seam when their JAR + matching
 *       {@code META-INF/services} entry are present.</li>
 * </ul>
 *
 * <p>Under the default Maven profile, no contributors are discovered and
 * the catalog equals {@code BaseFixtureCatalog.specs()}. Under
 * {@code -Pfixtures-jdk<N>}, the forward module's JAR adds its
 * contributor and the catalog grows.</p>
 *
 * <p>See {@code documentation/coveragex-multi-jdk-fixtures-design.md} §4
 * for the design rationale.</p>
 */
public final class FixtureCatalog {

    private FixtureCatalog() {}

    private static final List<FixtureContractSpec> ALL = loadAll();

    private static List<FixtureContractSpec> loadAll() {
        List<FixtureContractSpec> all = new ArrayList<>(BaseFixtureCatalog.specs());
        ServiceLoader.load(FixtureCatalogContributor.class)
                .forEach(contributor -> all.addAll(contributor.specs()));
        return List.copyOf(all);
    }

    public static List<FixtureContractSpec> all() {
        return ALL;
    }
}

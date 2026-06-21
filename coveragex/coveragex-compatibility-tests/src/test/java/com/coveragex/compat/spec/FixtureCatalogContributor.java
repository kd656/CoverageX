package com.coveragex.compat.spec;

import java.util.List;

/**
 * SPI for contributing fixture specs to the {@link FixtureCatalog} at runtime.
 *
 * <p>Each fixture source module that needs catalog representation ships an
 * implementation discovered via {@link java.util.ServiceLoader}. The base
 * 56 fixtures live in {@link BaseFixtureCatalog} and are added directly by
 * {@link FixtureCatalog}; forward modules (per-JDK fixture sources whose
 * syntax the floor JDK cannot parse) ship their own contributors that are
 * loaded only when their JAR is on the classpath.</p>
 *
 * <p>See {@code documentation/coveragex-multi-jdk-fixtures-design.md} §4 for
 * the design rationale.</p>
 */
public interface FixtureCatalogContributor {

    /**
     * Returns the specs this contributor adds to the catalog.
     *
     * <p>Each invocation should return the same list. The catalog
     * snapshots the result at class-load time and caches it.</p>
     */
    List<FixtureContractSpec> specs();
}

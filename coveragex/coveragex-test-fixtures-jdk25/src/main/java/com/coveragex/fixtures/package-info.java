/**
 * Forward fixtures requiring JDK 25 or later to compile.
 *
 * <p>This package will hold fixtures whose source syntax the JDK 21 floor
 * cannot parse. PR B (scaffolding) ships an empty package; the first
 * actual forward fixture lands in PR D once a JDK 25+ language feature
 * worth covering is identified.</p>
 *
 * <p>See {@code documentation/coveragex-multi-jdk-fixtures-design.md} §3.</p>
 */
package com.coveragex.fixtures;

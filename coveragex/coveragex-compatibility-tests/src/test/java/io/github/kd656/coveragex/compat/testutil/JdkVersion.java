package io.github.kd656.coveragex.compat.testutil;

/**
 * Resolves the active fixture JDK for the current test JVM.
 *
 * <p>Reads {@code fixture.jdk} (set by Surefire from the active matrix profile;
 * see the compatibility-tests POM). Falls back to the runtime feature version
 * when unset, which is the case for ad-hoc local runs that don't pass {@code -P}.</p>
 */
public final class JdkVersion {

    public static int current() {
        String prop = System.getProperty("fixture.jdk");
        if (prop != null && !prop.isBlank()) {
            return Integer.parseInt(prop);
        }
        return Runtime.version().feature();
    }

    private JdkVersion() {}
}

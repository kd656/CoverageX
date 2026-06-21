package io.github.kd656.coveragex.compat.testutil;

import java.util.Map;

/**
 * Classloader that defines fixture classes from in-memory instrumented bytes.
 *
 * <p><b>Child-first for fixture classes.</b> The standard {@link ClassLoader}
 * contract delegates {@code loadClass()} to the parent before calling
 * {@code findClass()}. Since the fixture jar is on the test classpath (parent
 * loader), the parent would resolve {@code io.github.kd656.coveragex.fixtures.IfElse} to
 * the *uninstrumented* original bytes and the instrumented version would never
 * run. This loader inverts that order for the fixture package only — everything
 * else (JDK, framework classes the injected probes call into, etc.) stays
 * parent-first so types remain compatible across the loader boundary.</p>
 */
public final class ByteArrayClassLoader extends ClassLoader {

    private static final String FIXTURE_PACKAGE = "io.github.kd656.coveragex.fixtures.";

    private final Map<String, byte[]> classes;

    public ByteArrayClassLoader(Map<String, byte[]> classes) {
        super(ByteArrayClassLoader.class.getClassLoader());
        this.classes = classes;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (name.startsWith(FIXTURE_PACKAGE)) {
            synchronized (getClassLoadingLock(name)) {
                Class<?> loadedClass = findLoadedClass(name);
                if (loadedClass == null) {
                    byte[] bytes = classes.get(name.replace('.', '/'));
                    if (bytes != null) {
                        loadedClass = defineClass(name, bytes, 0, bytes.length);
                    } else {
                        loadedClass = super.loadClass(name, false);
                    }
                }
                if (resolve) {
                    resolveClass(loadedClass);
                }
                return loadedClass;
            }
        }
        return super.loadClass(name, resolve);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] bytes = classes.get(name.replace('.', '/'));
        if (bytes == null) {
            throw new ClassNotFoundException(name);
        }

        return defineClass(name, bytes, 0, bytes.length);
    }
}

package io.github.kd656.coveragex.gradle.internal;

import java.io.File;

/**
 * Small shared helper for picking a production-classes directory out of a Gradle
 * {@code classesDirs} file collection. Callers apply their own fallback when no
 * directory exists yet (e.g. a not-yet-compiled subproject).
 */
public final class ClassesDirs {

    private ClassesDirs() {}

    /** First entry that exists as a directory, or {@code null} if none do. */
    public static File firstExisting(Iterable<File> dirs) {
        for (File dir : dirs) {
            if (dir.isDirectory()) {
                return dir;
            }
        }
        return null;
    }
}

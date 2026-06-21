package io.github.kd656.coveragex.core.analysis.source.model;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Unique key identifying a method within a class by its name and JVM descriptor.
 *
 * <p>This record doubles as a JSON map key. Jackson serializes it via {@link #toString()}
 * (producing {@code "name|descriptor"}) and deserializes it via the {@link #fromKey} factory.</p>
 */
public record MethodReference(

        // "<init>" for constructors
        String name,

        // JVM descriptor, e.g. "(Ljava/lang/String;)V"
        String descriptor
) {

    /**
     * Returns the canonical JSON map-key form: {@code "name|descriptor"}.
     *
     * <p>The {@code |} separator is safe because JVM method names never contain it
     * and descriptors use only {@code L}, {@code ;}, {@code (}, {@code )}, {@code [},
     * and primitive letters.</p>
     */
    @Override
    public String toString() {
        return name + "|" + descriptor;
    }

    /**
     * Jackson key-deserializer factory. Reconstructs a {@link MethodReference} from
     * the canonical string produced by {@link #toString()}.
     *
     * @param key serialized key in {@code "name|descriptor"} format
     * @return the reconstructed {@link MethodReference}
     * @throws IllegalArgumentException if the key does not contain the {@code |} separator
     */
    @JsonCreator
    public static MethodReference fromKey(String key) {
        int idx = key.indexOf('|');
        if (idx < 0) {
            throw new IllegalArgumentException(
                    "Invalid MethodReference key format (expected 'name|descriptor'): " + key);
        }
        return new MethodReference(key.substring(0, idx), key.substring(idx + 1));
    }
}

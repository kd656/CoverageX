package io.github.kd656.coveragex.core.instrument.stubs;

/**
 * Stub type used by method-call capture tests.
 * Represents a container that accepts two keys for membership checks.
 */
public final class MultiArgContainer {

    private final String key1;
    private final String key2;

    /**
     * Constructs a container with the given keys.
     *
     * @param key1 the first key
     * @param key2 the second key
     */
    public MultiArgContainer(String key1, String key2) {
        this.key1 = key1;
        this.key2 = key2;
    }

    /**
     * Returns {@code true} when both supplied keys match this container's keys.
     *
     * @param k the first key to check
     * @param v the second key to check
     * @return {@code true} if {@code k} equals {@code key1} and {@code v} equals {@code key2}
     */
    public boolean contains(String k, String v) {
        return key1.equals(k) && key2.equals(v);
    }

    @Override
    public String toString() {
        return "{}";
    }
}

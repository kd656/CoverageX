package io.github.kd656.coveragex.core.instrument.stubs;

/**
 * Stub flag provider used by nested-call matching tests.
 * Returns a {@code boolean} so that the inner-call boolean-return guard test
 * can verify that the capture guard fires on the outer call, not on this one.
 */
public final class FlagProvider {

    private final boolean value;

    /**
     * Constructs a provider that always returns {@code value}.
     *
     * @param value the boolean value to return from {@link #flag()}
     */
    public FlagProvider(boolean value) {
        this.value = value;
    }

    /**
     * Returns the fixed boolean value of this provider.
     *
     * @return the configured boolean value
     */
    public boolean flag() {
        return value;
    }

    @Override
    public String toString() {
        return "FP{}";
    }
}

package org.example.branches;

/**
 * Stub flag provider for {@link BranchCaptureShapes} nested-boolean-inner fixture.
 * Returns a configurable boolean value so that the inner-call boolean-return shape
 * can be demonstrated without introducing a dependency on real business logic.
 */
public final class FlagProvider {

    private final boolean value;

    /**
     * Constructs a provider that always returns {@code value}.
     *
     * @param value the boolean to return from {@link #flag()}
     */
    public FlagProvider(boolean value) {
        this.value = value;
    }

    /**
     * Returns the fixed boolean value of this provider.
     *
     * @return the configured flag value
     */
    public boolean flag() {
        return value;
    }

    @Override
    public String toString() {
        return "FlagProvider[" + value + "]";
    }
}

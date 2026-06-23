package io.github.kd656.coveragex.core.instrument.stubs;

/**
 * Stub rule engine used by nested-call matching tests.
 * Accepts a boolean flag and a string role; exercises the case where the inner
 * call returns {@code boolean} but must not trigger the capture guard.
 */
public final class FlagRules {

    /**
     * Returns {@code true} when {@code flag} is {@code true} and {@code role}
     * is not blank.
     *
     * @param flag a boolean flag from an inner call
     * @param role the role to check
     * @return {@code true} if access is granted
     */
    public boolean accepts(boolean flag, String role) {
        return flag && role != null && !role.isBlank();
    }

    @Override
    public String toString() {
        return "{}";
    }
}

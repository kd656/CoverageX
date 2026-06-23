package io.github.kd656.coveragex.core.instrument.stubs;

/**
 * Stub type used by nested-call matching tests.
 * Provides a two-argument instance method so that name+arity matching can be verified
 * against inner calls that share the same bytecode stream.
 */
public final class AccessService {

    /**
     * Returns {@code true} when {@code user} is non-null and {@code role} is
     * {@code "admin"}.
     *
     * @param user the user object (non-null check only)
     * @param role the required role string
     * @return {@code true} if access is granted
     */
    public boolean canAccess(Object user, String role) {
        return user != null && "admin".equals(role);
    }

    /**
     * Returns a user token object. Used as a nested call argument in fixture
     * methods to exercise the name-guard matching logic.
     *
     * @return a non-null user token
     */
    public Object resolveUser() {
        return "User#42";
    }

    @Override
    public String toString() {
        return "{}";
    }
}

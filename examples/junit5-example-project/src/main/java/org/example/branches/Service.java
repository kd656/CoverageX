package org.example.branches;

/**
 * Stub service type for {@link BranchCaptureShapes} demonstration fixtures.
 * Provides a two-argument instance method to exercise nested-call matching.
 */
public final class Service {

    /**
     * Returns {@code true} when {@code user} is non-null and {@code role} is "admin".
     *
     * @param user the user object
     * @param role the required role
     * @return {@code true} if access is granted
     */
    public boolean canAccess(Object user, String role) {
        return user != null && "admin".equals(role);
    }

    @Override
    public String toString() {
        return "Service{}";
    }
}

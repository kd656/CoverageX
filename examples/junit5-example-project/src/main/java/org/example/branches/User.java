package org.example.branches;

/**
 * Stub user type for {@link BranchCaptureShapes} nested-call fixtures.
 * Carries an ID so that captured values are meaningfully distinguishable in the report.
 */
public final class User {

    private final int id;

    /**
     * Constructs a user with the given ID.
     *
     * @param id the user's identifier
     */
    public User(int id) {
        this.id = id;
    }

    /**
     * Returns the user's ID.
     *
     * @return the user ID
     */
    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return "User#" + id;
    }
}

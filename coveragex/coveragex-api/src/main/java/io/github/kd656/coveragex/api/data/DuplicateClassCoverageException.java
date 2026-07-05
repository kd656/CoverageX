package io.github.kd656.coveragex.api.data;

/**
 * Thrown by {@link ExecutionData#merge(java.util.List)} when the same FQCN appears
 * in two of the merged sources.
 *
 * <p>The multi-module aggregator's ownership index normally routes each FQCN to a
 * single owner scope, so post-routing merge should never see duplicates. When it
 * does, the cause is almost always a real build problem — the same class compiled
 * into two modules' {@code target/classes}, or a code-generation plugin emitting
 * the same FQCN from two projects. Failing loudly surfaces the issue instead of
 * silently picking one contributor.</p>
 */
public final class DuplicateClassCoverageException extends RuntimeException {

    private final String classId;
    private final String ownerA;
    private final String ownerB;

    public DuplicateClassCoverageException(String classId, String ownerA, String ownerB) {
        super("Class " + classId + " is present in both " + ownerA + " and " + ownerB
                + "; post-routing merge expected a single owner per FQCN.");
        this.classId = classId;
        this.ownerA = ownerA;
        this.ownerB = ownerB;
    }

    public String classId() {
        return classId;
    }

    public String ownerA() {
        return ownerA;
    }

    public String ownerB() {
        return ownerB;
    }
}

package io.github.kd656.coveragex.compat.spec;

/**
 * One fixture's complete set of contract expectations.
 *
 * <p>Each fixture in the catalog implements this interface as a small,
 * dedicated class. The runner iterates {@link FixtureCatalog#all()} and
 * verifies every populated dimension declared in {@link #contracts()}.</p>
 *
 * <p>The instance-per-fixture shape (rather than per-fixture utility classes
 * with static factories) gives us polymorphism — the catalog is iterable,
 * shared patterns can be lifted into abstract base specs, and adding a new
 * contract dimension is a builder-level change rather than a hunt across 56
 * {@code Arguments.of(...)} sites.</p>
 */
public interface FixtureContractSpec {

    /** Fully-qualified fixture class name (e.g. {@code io.github.kd656.coveragex.fixtures.IfElse}). */
    String fqn();

    /** The five-dimension contract bundle for this fixture. */
    FixtureContracts contracts();

    /**
     * Ordered list of method calls the runner should make to drive the
     * fixture, each annotated with a test-context label.
     *
     * <p>Default: empty — the runner falls back to invoking
     * {@code execute()} once with no context. Specs that pin a
     * {@link io.github.kd656.coveragex.compat.contract.TestAttributionContract} override
     * this to drive the fixture's methods directly under deliberately-chosen
     * contexts.</p>
     */
    default java.util.List<InvocationStep> invocationPlan() {
        return java.util.List.of();
    }
}

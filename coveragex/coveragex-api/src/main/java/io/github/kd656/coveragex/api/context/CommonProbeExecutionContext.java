package io.github.kd656.coveragex.api.context;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class CommonProbeExecutionContext implements ProbeExecutionContext {

    private final String id;
    private final Map<ContextKey<?>, Object> attributes;

    public CommonProbeExecutionContext(String id, Map<ContextKey<?>, Object> attributes) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.attributes = Map.copyOf(attributes);
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(ContextKey<T> key) {
        return Optional.ofNullable((T) attributes.get(key));
    }

    @Override
    public Set<ContextKey<?>> keys() {
        return attributes.keySet();
    }

    /**
     * Equality is based on {@code id} only, not {@code attributes}.
     * {@code id} is the deduplication key — two contexts representing the same test
     * invocation with the same id are considered identical regardless of their attribute maps.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CommonProbeExecutionContext that)) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "CommonProbeExecutionContext{id=" + id + "}";
    }
}

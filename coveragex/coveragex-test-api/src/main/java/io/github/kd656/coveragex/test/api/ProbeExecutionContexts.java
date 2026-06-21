package io.github.kd656.coveragex.test.api;

import io.github.kd656.coveragex.api.context.ContextKey;
import io.github.kd656.coveragex.api.context.CommonProbeExecutionContext;
import io.github.kd656.coveragex.api.context.ProbeExecutionContext;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class ProbeExecutionContexts {

    private ProbeExecutionContexts() {}

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final String id;
        private final Map<ContextKey<?>, Object> attributes = new LinkedHashMap<>();

        private Builder(String id) {
            this.id = Objects.requireNonNull(id, "id must not be null");
        }

        public <T> Builder put(ContextKey<T> key, T value) {
            Objects.requireNonNull(key, "key must not be null");
            Objects.requireNonNull(value, "value must not be null");
            attributes.put(key, value);
            return this;
        }

        public ProbeExecutionContext build() {
            return new CommonProbeExecutionContext(id, Map.copyOf(attributes));
        }
    }
}

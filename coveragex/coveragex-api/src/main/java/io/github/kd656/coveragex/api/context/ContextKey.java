package io.github.kd656.coveragex.api.context;

public record ContextKey<T>(String name, Class<T> type) {

    public static <T> ContextKey<T> of(String name, Class<T> type) {
        return new ContextKey<>(name, type);
    }
}

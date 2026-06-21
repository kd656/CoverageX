package com.coveragex.core.analysis.source.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * In-memory model for a single Java class, holding per-method source metadata.
 * The {@code @JsonCreator} annotation allows Jackson to reconstruct the object during
 * deserialization in the agent phase.</p>
 */
public final class ClassModel {
    private final String className;
    private final String sourceFile;

    private final Map<MethodReference, MethodModel> methods = new LinkedHashMap<>();

    /**
     * Creates a new {@link ClassModel}.
     *
     * <p>Annotated with {@code @JsonCreator} so Jackson can instantiate this class
     * when reading {@code coveragex.map.json} in the agent phase.</p>
     *
     * @param className  internal class name (e.g. {@code com/example/MyClass})
     * @param sourceFile simple source file name (e.g. {@code MyClass.java})
     */
    @JsonCreator
    public ClassModel(@JsonProperty("className") String className,
                      @JsonProperty("sourceFile") String sourceFile) {
        this.className = Objects.requireNonNull(className);
        this.sourceFile = Objects.requireNonNull(sourceFile);
    }

    public MethodModel getOrCreate(MethodReference methodReference, Supplier<MethodModel> supplier) {
        return methods.computeIfAbsent(methodReference, (r) -> supplier.get());
    }

    public Map<MethodReference, MethodModel> getMethods() {
        return methods;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public String getClassName() {
        return className;
    }
}

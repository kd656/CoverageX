package io.github.kd656.coveragex.core.analysis.source.model;

import java.util.*;

/**
 * In-memory model that will be serialized into coveragex.map.
 * Keep it simple: class -> method -> decisions.
 */
public final class SemanticIndex {

    /** Format version for coveragex.map */
    public final int version = 1;

    /** internal-form class name (e.g. {@code com/example/Foo}) → {@link ClassModel}. */
    private final Map<String, ClassModel> classes = new LinkedHashMap<>();

    public ClassModel getOrCreateClass(String className, String sourceFile) {
        return classes.computeIfAbsent(className, k -> new ClassModel(k, sourceFile));
    }

    public void addDecision(String className,
                            String sourceFile,
                            String methodName,
                            String methodDescriptor,
                            DecisionModel decision) {
        ClassModel cm = getOrCreateClass(className, sourceFile);
        MethodModel methodModel = cm.getMethods()
                .get(new MethodReference(methodName, methodDescriptor));

        if (Objects.nonNull(methodModel)) {
            methodModel.addDecision(decision);
        }
    }

    public Map<String, ClassModel> getClasses() {
        return classes;
    }
}

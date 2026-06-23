package io.github.kd656.coveragex.core.analysis.source.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * In-memory model for a single method, holding source-level metadata and the list of
 * branch decisions ({@link DecisionModel}) detected during the analyze phase.
 *
 * <p>Instances are produced by the analyze phase (Maven plugin) and round-tripped
 * through {@code coveragex.map.json} to the agent phase. The {@link #fromJson} factory
 * acts as the Jackson {@code @JsonCreator} so that all {@code private final} fields can
 * be populated during deserialization without exposing mutable setters.</p>
 *
 * <p>Derived / computed properties ({@code id}, {@code method}, {@code hasDescriptor})
 * are excluded from JSON via {@code @JsonIgnore} to avoid round-trip ambiguity.</p>
 */
public final class MethodModel {

    /** Stable hash used for internal indexing; not persisted in JSON. */
    private final int id;

    /** Internal class name (e.g. {@code com/example/MyClass}); stored to recompute {@link #id}. */
    private final String className;

    private final String name;
    private final String descriptor;

    // Source-level metadata (fallback + reporting)
    private final int parametersCount;
    private final List<String> parametersTypes;
    private final String returnType;

    // Modifiers
    private final boolean isStatic;
    private final boolean isPublic;
    private final boolean isPrivate;
    private final boolean isProtected;
    private final boolean isAbstract;

    // Required for overload disambiguation
    private final int startLine;
    private final int endLine;

    // True if isConstructor
    private final boolean isConstructor;

    /**
     * Source-level parameter names in declaration order.
     * Empty when the method has no parameters or when the source map was not available.
     */
    private final List<String> parameterNames;

    private final List<DecisionModel> decisionsList = new ArrayList<>();

    private MethodModel(String className,
                        String name, String descriptor,
                        int parametersCount,
                        List<String> parametersTypes,
                        String returnType,
                        boolean isStatic,
                        boolean isPublic,
                        boolean isPrivate,
                        boolean isProtected,
                        boolean isAbstract,
                        int startLine,
                        int endLine,
                        boolean isConstructor,
                        List<String> parameterNames) {
        this.className = className;
        this.name = name;
        this.id = generateMethodId(className);
        this.descriptor = descriptor;
        this.parametersCount = parametersCount;
        this.parametersTypes = parametersTypes;
        this.returnType = returnType;
        this.isStatic = isStatic;
        this.isPublic = isPublic;
        this.isPrivate = isPrivate;
        this.isProtected = isProtected;
        this.isAbstract = isAbstract;
        this.startLine = startLine;
        this.endLine = endLine;
        this.isConstructor = isConstructor;
        this.parameterNames = List.copyOf(parameterNames);
    }

    /**
     * Creates a regular (non-constructor) method model.
     *
     * @param className       internal class name used to compute the stable {@link #id}
     * @param name            method name
     * @param descriptor      JVM descriptor (e.g. {@code (Ljava/lang/String;)V})
     * @param parametersCount number of declared parameters
     * @param parametersTypes list of parameter type names
     * @param returnType      return type name
     * @param isStatic        {@code true} if the method is {@code static}
     * @param isPublic        {@code true} if the method is {@code public}
     * @param isPrivate       {@code true} if the method is {@code private}
     * @param isProtected     {@code true} if the method is {@code protected}
     * @param isAbstract      {@code true} if the method is {@code abstract}
     * @param startLine       first source line of the method body
     * @param endLine         last source line of the method body
     * @param parameterNames  source-level parameter names in declaration order;
     *                        empty when not available
     * @return a new {@link MethodModel}
     */
    public static MethodModel createMethod(String className,
                                           String name, String descriptor,
                                           int parametersCount,
                                           List<String> parametersTypes,
                                           String returnType,
                                           boolean isStatic,
                                           boolean isPublic,
                                           boolean isPrivate,
                                           boolean isProtected,
                                           boolean isAbstract,
                                           int startLine,
                                           int endLine,
                                           List<String> parameterNames) {
        return new MethodModel(className, name, descriptor, parametersCount, parametersTypes,
                returnType, isStatic, isPublic, isPrivate, isProtected, isAbstract,
                startLine, endLine, false, parameterNames);
    }

    /**
     * Creates a constructor method model.
     *
     * @param className       internal class name used to compute the stable {@link #id}
     * @param descriptor      JVM descriptor of the constructor
     * @param parametersCount number of declared parameters
     * @param parametersTypes list of parameter type names
     * @param isPublic        {@code true} if the constructor is {@code public}
     * @param isPrivate       {@code true} if the constructor is {@code private}
     * @param isProtected     {@code true} if the constructor is {@code protected}
     * @param startLine       first source line of the constructor body
     * @param endLine         last source line of the constructor body
     * @param parameterNames  source-level parameter names in declaration order;
     *                        empty when not available
     * @return a new {@link MethodModel}
     */
    public static MethodModel createConstructor(String className,
                                                String descriptor,
                                                int parametersCount,
                                                List<String> parametersTypes,
                                                boolean isPublic,
                                                boolean isPrivate,
                                                boolean isProtected,
                                                int startLine,
                                                int endLine,
                                                List<String> parameterNames) {
        return new MethodModel(className, "<init>", descriptor, parametersCount, parametersTypes,
                "void", false, isPublic, isPrivate, isProtected, false,
                startLine, endLine, true, parameterNames);
    }

    /**
     * Jackson deserialization factory. Reconstructs a {@link MethodModel} from the
     * JSON representation written by the analyze phase.
     *
     * <p>The {@code decisionsList} is populated by injecting the deserialized list
     * via {@link #addDecision} so that the internal {@code ArrayList} stays consistent.</p>
     *
     * @param className       internal class name (may be {@code null} for old map files;
     *                        the {@link #id} will differ but is not used by the agent)
     * @param name            method name
     * @param descriptor      JVM descriptor
     * @param parametersCount number of declared parameters
     * @param parametersTypes list of parameter type names
     * @param returnType      return type name
     * @param isStatic        modifier flag
     * @param isPublic        modifier flag
     * @param isPrivate       modifier flag
     * @param isProtected     modifier flag
     * @param isAbstract      modifier flag
     * @param startLine       first source line of the method body
     * @param endLine         last source line of the method body
     * @param isConstructor   {@code true} if this is a constructor
     * @param decisionsList   branch decisions detected during the analyze phase
     * @return the reconstructed {@link MethodModel}
     */
    @JsonCreator
    public static MethodModel fromJson(
            @JsonProperty("className") String className,
            @JsonProperty("name") String name,
            @JsonProperty("descriptor") String descriptor,
            @JsonProperty("parametersCount") int parametersCount,
            @JsonProperty("parametersTypes") List<String> parametersTypes,
            @JsonProperty("returnType") String returnType,
            @JsonProperty("static") boolean isStatic,
            @JsonProperty("public") boolean isPublic,
            @JsonProperty("private") boolean isPrivate,
            @JsonProperty("protected") boolean isProtected,
            @JsonProperty("abstract") boolean isAbstract,
            @JsonProperty("startLine") int startLine,
            @JsonProperty("endLine") int endLine,
            @JsonProperty("isConstructor") boolean isConstructor,
            @JsonProperty("parameterNames") List<String> parameterNames,
            @JsonProperty("decisionsList") List<DecisionModel> decisionsList) {

        MethodModel model = new MethodModel(
                className, name, descriptor, parametersCount,
                parametersTypes != null ? parametersTypes : List.of(),
                returnType, isStatic, isPublic, isPrivate, isProtected, isAbstract,
                startLine, endLine, isConstructor,
                parameterNames != null ? parameterNames : List.of());

        if (decisionsList != null) {
            decisionsList.forEach(model::addDecision);
        }
        return model;
    }

    /**
     * Returns a {@link MethodReference} that uniquely identifies this method within its class.
     *
     * @return a new {@link MethodReference} containing this method's name and descriptor
     */
    public MethodReference generateReference() {
        return new MethodReference(name, descriptor);
    }

    /**
     * Appends a {@link DecisionModel} to this method's branch decision list.
     *
     * @param decisionModel the decision to add; must not be {@code null}
     */
    public void addDecision(DecisionModel decisionModel) {
        decisionsList.add(Objects.requireNonNull(decisionModel));
    }

    private int generateMethodId(String classInternalName) {
        // If descriptor exists, it should dominate stability
        if (Objects.nonNull(descriptor) && !descriptor.isBlank()) {
            return Objects.hash(classInternalName, name, descriptor);
        }
        // fallback
        return Objects.hash(classInternalName, name, parametersCount, startLine, endLine);
    }

    /**
     * Returns the stable internal identifier for this method.
     *
     * <p>Excluded from JSON serialization because it is derived from {@link #getClassName()},
     * {@link #getName()}, and {@link #getDescriptor()}, which are already persisted.</p>
     *
     * @return the computed hash-based identifier
     */
    @JsonIgnore
    public int getId() {
        return id;
    }

    /**
     * Returns the internal class name used to compute this method's {@link #getId()}.
     *
     * @return internal class name (e.g. {@code com/example/MyClass})
     */
    public String getClassName() {
        return className;
    }

    /** @return {@code true} if this model was created with {@link #createMethod} */
    @JsonIgnore
    public boolean isMethod() {
        return !isConstructor;
    }

    /** @return {@code true} if {@link #getDescriptor()} is non-null and non-blank */
    @JsonIgnore
    public boolean hasDescriptor() {
        return descriptor != null && !descriptor.isBlank();
    }

    public List<DecisionModel> getDecisionsList() {
        return decisionsList;
    }

    @JsonProperty("isConstructor")
    public boolean isConstructor() {
        return isConstructor;
    }

    public String getName() {
        return name;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public int getParametersCount() {
        return parametersCount;
    }

    public List<String> getParametersTypes() {
        return parametersTypes;
    }

    public String getReturnType() {
        return returnType;
    }

    @JsonProperty("static")
    public boolean isStatic() {
        return isStatic;
    }

    @JsonProperty("public")
    public boolean isPublic() {
        return isPublic;
    }

    @JsonProperty("private")
    public boolean isPrivate() {
        return isPrivate;
    }

    @JsonProperty("protected")
    public boolean isProtected() {
        return isProtected;
    }

    @JsonProperty("abstract")
    public boolean isAbstract() {
        return isAbstract;
    }

    public int getStartLine() {
        return startLine;
    }

    public int getEndLine() {
        return endLine;
    }

    /**
     * Returns the source-level parameter names in declaration order.
     *
     * <p>Populated when the method was built from a parsed AST node via
     * {@link #createMethod} or {@link #createConstructor}. Empty when the
     * method has no parameters or when no source map was available at
     * analysis time.</p>
     *
     * @return immutable list of parameter names; never {@code null}
     */
    public List<String> getParameterNames() {
        return parameterNames;
    }
}

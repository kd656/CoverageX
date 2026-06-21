package com.coveragex.core.analysis.source;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * DescriptorResolver resolves JVM descriptors for methods/constructors from JavaParser AST.
 * Usage:
 *   DescriptorResolver resolver = DescriptorResolver.defaultResolver();
 *   String desc = resolver.resolveMethodDescriptor(cu, methodDecl);
 *   String ctorDesc = resolver.resolveConstructorDescriptor(cu, ctorDecl);
 */
public final class DescriptorResolver {

    private final MethodDescriptorProvider methodDescriptorProvider;
    private final ConstructorDescriptorProvider constructorDescriptorProvider;

    private DescriptorResolver(MethodDescriptorProvider methodDescriptorProvider,
                              ConstructorDescriptorProvider constructorDescriptorProvider) {
        this.methodDescriptorProvider = Objects.requireNonNull(methodDescriptorProvider);
        this.constructorDescriptorProvider = Objects.requireNonNull(constructorDescriptorProvider);
    }

    public String resolveMethodDescriptor(CompilationUnit cu, MethodDeclaration methodDecl) {
        return methodDescriptorProvider.getMethodDescriptor(cu, methodDecl);
    }

    public String resolveConstructorDescriptor(CompilationUnit cu, ConstructorDeclaration ctorDecl) {
        return constructorDescriptorProvider.getConstructorDescriptor(cu, ctorDecl);
    }

    /**
     * Default implementation:
     * - Tries SymbolSolver first (if available and configured).
     * - Falls back to AST-based manual descriptor building.
     */
    public static DescriptorResolver defaultResolver() {
        TypeNameResolver typeNameResolver = new CompositeTypeNameResolver(List.of(
                new ImportedTypeNameResolver(),
                new JavaLangTypeNameResolver(),
                new SamePackageTypeNameResolver()
        ));
        ManualDescriptorBuilder manual = new ManualDescriptorBuilder(typeNameResolver);

        return new DescriptorResolver(
                new ChainedMethodDescriptorProvider(
                        List.of(new SymbolSolverMethodDescriptorProvider(), manual)
                ),
                new ChainedConstructorDescriptorProvider(
                        List.of(new SymbolSolverConstructorDescriptorProvider(), manual)
                )
        );
    }

    private interface MethodDescriptorProvider {
        String getMethodDescriptor(CompilationUnit cu, MethodDeclaration md);
    }

    private interface ConstructorDescriptorProvider {
        String getConstructorDescriptor(CompilationUnit cu, ConstructorDeclaration cd);
    }

    private interface TypeNameResolver {
        /**
         * @return fully qualified name (e.g. java.util.List), or empty if cannot resolve.
         */
        Optional<String> resolveFqcn(CompilationUnit cu, ClassOrInterfaceType type);
    }

    private static final class ChainedMethodDescriptorProvider implements MethodDescriptorProvider {
        private final List<MethodDescriptorProvider> chain;

        public ChainedMethodDescriptorProvider(List<MethodDescriptorProvider> chain) {
            this.chain = List.copyOf(chain);
        }

        @Override
        public String getMethodDescriptor(CompilationUnit cu, MethodDeclaration md) {
            for (MethodDescriptorProvider p : chain) {
                String d = safe(() -> p.getMethodDescriptor(cu, md));
                if (d != null && !d.isBlank()) return d;
            }
            return null;
        }
    }

    private static final class ChainedConstructorDescriptorProvider implements ConstructorDescriptorProvider {
        private final List<ConstructorDescriptorProvider> chain;

        public ChainedConstructorDescriptorProvider(List<ConstructorDescriptorProvider> chain) {
            this.chain = List.copyOf(chain);
        }

        @Override
        public String getConstructorDescriptor(CompilationUnit cu, ConstructorDeclaration cd) {
            for (ConstructorDescriptorProvider p : chain) {
                String d = safe(() -> p.getConstructorDescriptor(cu, cd));
                if (d != null && !d.isBlank()) return d;
            }
            return null;
        }
    }

    /**
     * Best-effort provider. Returns null if SymbolSolver isn't configured or resolution fails.
     */
    private static final class SymbolSolverMethodDescriptorProvider implements MethodDescriptorProvider {
        @Override
        public String getMethodDescriptor(CompilationUnit cu, MethodDeclaration md) {
            try {
                return md.resolve().toDescriptor();
            } catch (Throwable throwable) {
                return null;
            }
        }
    }

    /**
     * Best-effort provider. Returns null if SymbolSolver isn't configured or resolution fails.
     */
    public static final class SymbolSolverConstructorDescriptorProvider implements ConstructorDescriptorProvider {
        @Override
        public String getConstructorDescriptor(CompilationUnit cu, ConstructorDeclaration cd) {
            try {
                return cd.toDescriptor();
            } catch (Throwable ignored) {
                return null;
            }
        }
    }

    /**
     * ManualDescriptorBuilder can serve as both MethodDescriptorProvider and ConstructorDescriptorProvider.
     * It builds JVM descriptors from AST + simple name resolution strategies.
     */
    private static final class ManualDescriptorBuilder implements MethodDescriptorProvider, ConstructorDescriptorProvider {

        private final TypeNameResolver typeNameResolver;

        ManualDescriptorBuilder(TypeNameResolver typeNameResolver) {
            this.typeNameResolver = Objects.requireNonNull(typeNameResolver);
        }

        @Override
        public String getMethodDescriptor(CompilationUnit cu, MethodDeclaration md) {
            StringBuilder sb = new StringBuilder();
            sb.append("(");
            for (Parameter p : md.getParameters()) {
                sb.append(toJvmType(cu, p.getType()));
            }
            sb.append(")");
            sb.append(toJvmType(cu, md.getType()));
            return sb.toString();
        }

        @Override
        public String getConstructorDescriptor(CompilationUnit cu, ConstructorDeclaration cd) {
            StringBuilder sb = new StringBuilder();
            sb.append("(");
            for (Parameter p : cd.getParameters()) {
                sb.append(toJvmType(cu, p.getType()));
            }
            sb.append(")V");
            return sb.toString();
        }

        private String toJvmType(CompilationUnit cu, Type type) {
            if (type.isPrimitiveType()) {
                return switch (type.asPrimitiveType().getType()) {
                    case BOOLEAN -> "Z";
                    case BYTE    -> "B";
                    case CHAR    -> "C";
                    case SHORT   -> "S";
                    case INT     -> "I";
                    case LONG    -> "J";
                    case FLOAT   -> "F";
                    case DOUBLE  -> "D";
                };
            }

            if (type.isVoidType()) return "V";

            if (type.isArrayType()) {
                return "[" + toJvmType(cu, type.asArrayType().getComponentType());
            }

            if (type.isClassOrInterfaceType()) {
                ClassOrInterfaceType t = type.asClassOrInterfaceType();
                String fqcn = typeNameResolver.resolveFqcn(cu, t)
                        .orElseGet(t::getNameAsString); // last-resort fallback
                return "L" + fqcn.replace('.', '/') + ";";
            }

            // Wildcard, intersection, var, unknown: treat as Object
            return "Ljava/lang/Object;";
        }
    }

    private static final class CompositeTypeNameResolver implements TypeNameResolver {
        private final List<TypeNameResolver> resolvers;

        CompositeTypeNameResolver(List<TypeNameResolver> resolvers) {
            this.resolvers = List.copyOf(resolvers);
        }

        @Override
        public Optional<String> resolveFqcn(CompilationUnit cu, ClassOrInterfaceType type) {
            // If it's already qualified: java.util.List
            String simple = type.getNameAsString();
            if (simple.contains(".")) return Optional.of(simple);

            for (TypeNameResolver r : resolvers) {
                Optional<String> fq = r.resolveFqcn(cu, type);
                if (fq.isPresent()) return fq;
            }
            return Optional.empty();
        }
    }

    /** Resolves by explicit imports: import java.util.List; */
    public static final class ImportedTypeNameResolver implements TypeNameResolver {
        @Override
        public Optional<String> resolveFqcn(CompilationUnit cu, ClassOrInterfaceType type) {
            String simple = type.getNameAsString();
            for (ImportDeclaration imp : cu.getImports()) {
                if (imp.isAsterisk()) continue;
                String name = imp.getNameAsString();
                if (name.endsWith("." + simple)) return Optional.of(name);
            }
            return Optional.empty();
        }
    }

    /** Resolves java.lang.* common types. */
    public static final class JavaLangTypeNameResolver implements TypeNameResolver {
        @Override
        public Optional<String> resolveFqcn(CompilationUnit cu, ClassOrInterfaceType type) {
            String simple = type.getNameAsString();
            // cheap allow-list to avoid reflection cost
            return switch (simple) {
                case "String", "Object", "Integer", "Long", "Short", "Byte",
                     "Boolean", "Character", "Double", "Float", "Void",
                     "RuntimeException", "Exception", "Error", "Throwable" ->
                        Optional.of("java.lang." + simple);
                default -> Optional.empty();
            };
        }
    }

    /** Resolves as same package: package x.y; class Foo -> x.y.Foo */
    public static final class SamePackageTypeNameResolver implements TypeNameResolver {
        @Override
        public Optional<String> resolveFqcn(CompilationUnit cu, ClassOrInterfaceType type) {
            String pkg = cu.getPackageDeclaration().map(NodeWithName::getNameAsString).orElse("");
            if (pkg.isBlank()) return Optional.empty();
            return Optional.of(pkg + "." + type.getNameAsString());
        }
    }

    private interface SupplierWithThrow<T> { T get() throws Exception; }

    private static <T> T safe(SupplierWithThrow<T> s) {
        try { return s.get(); } catch (Exception e) { return null; }
    }
}

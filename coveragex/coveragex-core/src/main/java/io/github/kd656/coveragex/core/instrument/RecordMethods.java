package io.github.kd656.coveragex.core.instrument;

import org.objectweb.asm.Opcodes;

import java.util.Map;

/**
 * Predicates for skipping compiler-generated members on {@code record} classes.
 *
 * <p>Records auto-generate {@code equals(Object)}, {@code hashCode()},
 * {@code toString()}, and one accessor per component. Their bytecode is
 * synthetic — nothing meaningful for coverage to measure, so the probe
 * pipeline skips them.</p>
 */
public final class RecordMethods {

    private RecordMethods() {}

    public static boolean isRecord(int classAccess) {
        return (classAccess & Opcodes.ACC_RECORD) != 0;
    }

    public static boolean isGeneratedObjectMethod(String name, String descriptor) {
        return ("equals".equals(name)   && "(Ljava/lang/Object;)Z".equals(descriptor))
            || ("hashCode".equals(name) && "()I".equals(descriptor))
            || ("toString".equals(name) && "()Ljava/lang/String;".equals(descriptor));
    }

    /**
     * True when the (name, descriptor) pair matches the compiler-generated
     * accessor for a component: zero-arg method named after a component
     * whose return descriptor equals the component's declared descriptor.
     *
     * <p>An explicit user override matches the same shape; callers that want
     * to preserve overrides must additionally check the method body (or a
     * source-map entry) before skipping.</p>
     */
    public static boolean isComponentAccessorShape(
            String name, String descriptor,
            Map<String, String> recordComponents) {
        String componentDescriptor = recordComponents.get(name);
        return componentDescriptor != null
            && descriptor.equals("()" + componentDescriptor);
    }
}

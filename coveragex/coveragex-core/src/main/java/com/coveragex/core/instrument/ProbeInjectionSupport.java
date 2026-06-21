package com.coveragex.core.instrument;

import com.coveragex.api.data.ProbeMetadata;
import com.coveragex.core.probe.ProbeMetadataVisitor;
import com.coveragex.core.probe.ProbeOpcodeSupport;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.coveragex.core.collect.CoverageDataCollectorDelegate.COLLECTOR_OWNER_CLASS;
import static com.coveragex.core.collect.CoverageDataCollectorDelegate.RECORD_HIT_DESCRIPTOR;

/**
 * Bytecode-emitting adapter for the shared probe metadata visitor.
 *
 * <p>{@link ProbeMetadataVisitor} decides where probes exist and which IDs they receive.
 * This class supplies the runtime side effect for each hook: emitting calls to the
 * collector while preserving the same metadata ordering used by static enrichment.</p>
 */
abstract class ProbeInjectionSupport extends ProbeMetadataVisitor {

    /** Internal class name (e.g. {@code org/example/Foo}). */
    protected final String className;

    /** JVM method descriptor (e.g. {@code (Ljava/lang/String;)V}). */
    protected final String descriptor;

    /** {@code true} if the method has the {@code ACC_STATIC} flag set. */
    protected final boolean isStatic;

    protected ProbeInjectionSupport(int api, MethodVisitor mv,
                                    String className, String methodName, String descriptor,
                                    int access,
                                    AtomicInteger probeCounter,
                                    List<ProbeMetadata> metadataAccumulator) {
        super(api, mv, methodName, probeCounter, metadataAccumulator);
        this.className = className;
        this.descriptor = descriptor;
        this.isStatic = (access & Opcodes.ACC_STATIC) != 0;
    }

    @Override
    protected void onMethodProbe(int probeId) {
        injectEntryProbeCall(probeId);
    }

    @Override
    protected void onReturnProbe(int probeId) {
        injectNonEntryProbeCall(probeId);
    }

    @Override
    protected void onThrowProbe(int probeId) {
        injectNonEntryProbeCall(probeId);
    }

    @Override
    protected void onSegmentProbe(int probeId) {
        injectNonEntryProbeCall(probeId);
    }

    @Override
    protected void onBranchProbe(int opcode, Label originalTarget,
                                 int fallThroughProbeId, int jumpTakenProbeId) {
        Label jumpTakenLabel = new Label();
        Label afterLabel = new Label();

        // Split the original conditional jump into explicit fall-through and jump-taken probe paths.
        emitJumpInsn(opcode, jumpTakenLabel);

        injectNonEntryProbeCall(fallThroughProbeId);
        emitJumpInsn(Opcodes.GOTO, afterLabel);

        emitLabel(jumpTakenLabel);
        injectNonEntryProbeCall(jumpTakenProbeId);
        emitJumpInsn(Opcodes.GOTO, originalTarget);

        emitLabel(afterLabel);
    }

    protected void injectEntryProbeCall(int probeId) {
        super.visitLdcInsn(className);
        super.visitLdcInsn(methodName);
        super.visitLdcInsn(probeId);
        buildArgsArray();
        super.visitMethodInsn(Opcodes.INVOKESTATIC, COLLECTOR_OWNER_CLASS, "recordHit",
                RECORD_HIT_DESCRIPTOR, false);
    }

    protected void injectNonEntryProbeCall(int probeId) {
        super.visitLdcInsn(className);
        super.visitLdcInsn(methodName);
        super.visitLdcInsn(probeId);
        super.visitInsn(Opcodes.ACONST_NULL);
        super.visitMethodInsn(Opcodes.INVOKESTATIC, COLLECTOR_OWNER_CLASS, "recordHit",
                RECORD_HIT_DESCRIPTOR, false);
    }

    protected void buildArgsArray() {
        Type[] argTypes = Type.getArgumentTypes(descriptor);
        int argCount = argTypes.length;

        super.visitLdcInsn(argCount);
        super.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");

        int slot = isStatic ? 0 : 1;
        for (int i = 0; i < argCount; i++) {
            Type argType = argTypes[i];
            super.visitInsn(Opcodes.DUP);
            super.visitLdcInsn(i);
            // JVM local slots are descriptor-shaped: long/double consume two slots, references one.
            loadAndBoxArgument(argType, slot);
            super.visitInsn(Opcodes.AASTORE);
            slot += argType.getSize();
        }
    }

    /**
     * Emits a load instruction for the argument at {@code slot}, followed by the
     * appropriate boxing method call so that primitive values are promoted to their
     * wrapper types before being stored in the {@code Object[]} args array.
     */
    protected void loadAndBoxArgument(Type type, int slot) {
        switch (type.getSort()) {
            case Type.BOOLEAN:
                super.visitVarInsn(Opcodes.ILOAD, slot);
                super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
                break;
            case Type.BYTE:
                super.visitVarInsn(Opcodes.ILOAD, slot);
                super.visitInsn(Opcodes.I2B);
                super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
                break;
            case Type.CHAR:
                super.visitVarInsn(Opcodes.ILOAD, slot);
                super.visitInsn(Opcodes.I2C);
                super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
                break;
            case Type.SHORT:
                super.visitVarInsn(Opcodes.ILOAD, slot);
                super.visitInsn(Opcodes.I2S);
                super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
                break;
            case Type.INT:
                super.visitVarInsn(Opcodes.ILOAD, slot);
                super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
                break;
            case Type.LONG:
                super.visitVarInsn(Opcodes.LLOAD, slot);
                super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
                break;
            case Type.FLOAT:
                super.visitVarInsn(Opcodes.FLOAD, slot);
                super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
                break;
            case Type.DOUBLE:
                super.visitVarInsn(Opcodes.DLOAD, slot);
                super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
                break;
            default:
                super.visitVarInsn(Opcodes.ALOAD, slot);
                break;
        }
    }

    protected String opcodeToConditionText(int opcode) {
        return ProbeOpcodeSupport.opcodeToConditionText(opcode);
    }
}

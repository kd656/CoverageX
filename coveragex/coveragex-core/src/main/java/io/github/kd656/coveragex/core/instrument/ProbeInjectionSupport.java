package io.github.kd656.coveragex.core.instrument;

import io.github.kd656.coveragex.api.data.ProbeMetadata;
import io.github.kd656.coveragex.core.probe.ProbeMetadataVisitor;
import io.github.kd656.coveragex.core.probe.ProbeOpcodeSupport;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static io.github.kd656.coveragex.core.collect.CoverageDataCollectorDelegate.BRANCH_HIT_DESCRIPTOR;
import static io.github.kd656.coveragex.core.collect.CoverageDataCollectorDelegate.COLLECTOR_OWNER_CLASS;
import static io.github.kd656.coveragex.core.collect.CoverageDataCollectorDelegate.METHOD_ENTRY_DESCRIPTOR;
import static io.github.kd656.coveragex.core.collect.CoverageDataCollectorDelegate.SIMPLE_HIT_DESCRIPTOR;

/**
 * Bytecode-emitting adapter for the shared probe metadata visitor.
 *
 * <p>{@link ProbeMetadataVisitor} decides where probes exist and which IDs they
 * receive. This class supplies the runtime side effect for each hook: emitting
 * calls to the collector while preserving the same metadata ordering used by
 * static enrichment.</p>
 *
 * <p>Each probe kind targets a dedicated entry point on
 * {@link io.github.kd656.coveragex.core.collect.CoverageDataCollectorDelegate}:</p>
 * <ul>
 *   <li>Method-entry → {@code recordMethodEntry} with boxed argument array.</li>
 *   <li>Branch → {@code recordBranchHit} with captured operand values when
 *       source analysis produced a capture mask, or an empty array otherwise.</li>
 *   <li>Return / throw / segment → {@code recordSimpleHit} with no payload.</li>
 * </ul>
 */
abstract class ProbeInjectionSupport extends ProbeMetadataVisitor {

    /** Internal class name (e.g. {@code org/example/Foo}). */
    protected final String className;

    /** JVM method descriptor (e.g. {@code (Ljava/lang/String;)V}). */
    protected final String descriptor;

    /** {@code true} if the method has the {@code ACC_STATIC} flag set. */
    protected final boolean isStatic;

    /**
     * The next available JVM local-variable slot for scratch locals allocated by
     * the capture emitter. Initialised in the constructor from the method descriptor
     * so that allocated slots are always above the parameter frame. ASM's
     * {@code COMPUTE_FRAMES} adjusts {@code maxLocals} as needed when writing the
     * class file.
     */
    private int nextLocalSlot;

    private static final Type OBJECT_TYPE = Type.getType(Object.class);

    protected ProbeInjectionSupport(int api, MethodVisitor mv,
                                    String className, String methodName, String descriptor,
                                    int access,
                                    AtomicInteger probeCounter,
                                    List<ProbeMetadata> metadataAccumulator) {
        super(api, mv, methodName, probeCounter, metadataAccumulator);
        this.className = className;
        this.descriptor = descriptor;
        this.isStatic = (access & Opcodes.ACC_STATIC) != 0;
        // Pre-compute the first free slot: 'this' (if instance) + all parameter slots.
        int slot = isStatic ? 0 : 1;
        for (Type t : Type.getArgumentTypes(descriptor)) {
            slot += t.getSize();
        }
        this.nextLocalSlot = slot;
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

    /**
     * Emits the two probe call-sites that flank a conditional jump: one for
     * the fall-through path and one for the jump-taken path. When
     * {@code operandLocals} is non-null, both call-sites are emitted with the
     * captured operand values via
     * {@link #injectBranchProbeCallWithLocals(int, int[])}; otherwise the
     * empty-array form is used.
     *
     * @param opcode             the original conditional-jump opcode
     * @param originalTarget     the original jump target label
     * @param fallThroughProbeId probe id for the fall-through direction
     * @param jumpTakenProbeId   probe id for the jump-taken direction
     * @param operandLocals      slot indices for stashed operand values, or
     *                           {@code null} when no capture was performed
     */
    @Override
    protected void onBranchProbe(int opcode, Label originalTarget,
                                 int fallThroughProbeId, int jumpTakenProbeId,
                                 int[] operandLocals) {
        Label jumpTakenLabel = new Label();
        Label afterLabel = new Label();

        emitJumpInsn(opcode, jumpTakenLabel);

        emitBranchProbe(fallThroughProbeId, operandLocals);
        emitJumpInsn(Opcodes.GOTO, afterLabel);

        emitLabel(jumpTakenLabel);
        emitBranchProbe(jumpTakenProbeId, operandLocals);
        emitJumpInsn(Opcodes.GOTO, originalTarget);

        emitLabel(afterLabel);
    }

    /**
     * Dispatches to {@link #injectBranchProbeCallWithLocals(int, int[])} when
     * operand locals are present, otherwise to {@link #injectBranchProbeCall(int)}.
     *
     * @param probeId       the probe id for this direction
     * @param operandLocals stashed operand slots, or {@code null}
     */
    private void emitBranchProbe(int probeId, int[] operandLocals) {
        if (operandLocals != null && operandLocals.length > 0) {
            injectBranchProbeCallWithLocals(probeId, operandLocals);
        } else {
            injectBranchProbeCall(probeId);
        }
    }

    /**
     * Emits a {@code recordMethodEntry} call that pushes the boxed argument
     * array before invoking the collector.
     *
     * @param probeId the probe id assigned to this method-entry probe
     */
    protected void injectEntryProbeCall(int probeId) {
        super.visitLdcInsn(className);
        super.visitLdcInsn(methodName);
        super.visitLdcInsn(probeId);
        buildArgsArray();
        super.visitMethodInsn(Opcodes.INVOKESTATIC, COLLECTOR_OWNER_CLASS,
                "recordMethodEntry", METHOD_ENTRY_DESCRIPTOR, false);
    }

    /**
     * Emits a {@code recordSimpleHit} call with no operand payload — used for
     * return, throw, and segment probes.
     *
     * @param probeId the probe id assigned to this probe
     */
    protected void injectNonEntryProbeCall(int probeId) {
        super.visitLdcInsn(className);
        super.visitLdcInsn(probeId);
        super.visitMethodInsn(Opcodes.INVOKESTATIC, COLLECTOR_OWNER_CLASS,
                "recordSimpleHit", SIMPLE_HIT_DESCRIPTOR, false);
    }

    /**
     * Emits a {@code recordBranchHit} call with an empty operand-values array.
     * Used when no capture was performed for the current operand (UNKNOWN kind,
     * or no source map available).
     *
     * @param probeId the probe id assigned to this branch direction
     */
    protected void injectBranchProbeCall(int probeId) {
        super.visitLdcInsn(className);
        super.visitLdcInsn(probeId);
        super.visitLdcInsn(0);
        super.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
        super.visitMethodInsn(Opcodes.INVOKESTATIC, COLLECTOR_OWNER_CLASS,
                "recordBranchHit", BRANCH_HIT_DESCRIPTOR, false);
    }

    /**
     * Emits a {@code recordBranchHit} call that builds an {@code Object[]} from
     * the given stashed local-variable slots. Each slot must hold a boxed reference
     * or boxed primitive — the raw value captured by the preceding capture emitter.
     * Serialisation to {@link String} happens later inside
     * {@link io.github.kd656.coveragex.core.collect.CommonCoverageDataCollector#attributeToTest}.
     *
     * @param probeId    the probe id assigned to this branch direction
     * @param valueSlots local-variable slot indices, one per captured operand value
     */
    protected void injectBranchProbeCallWithLocals(int probeId, int[] valueSlots) {
        super.visitLdcInsn(className);
        super.visitLdcInsn(probeId);
        super.visitLdcInsn(valueSlots.length);
        super.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
        for (int i = 0; i < valueSlots.length; i++) {
            super.visitInsn(Opcodes.DUP);
            super.visitLdcInsn(i);
            super.visitVarInsn(Opcodes.ALOAD, valueSlots[i]);
            super.visitInsn(Opcodes.AASTORE);
        }
        super.visitMethodInsn(Opcodes.INVOKESTATIC, COLLECTOR_OWNER_CLASS,
                "recordBranchHit", BRANCH_HIT_DESCRIPTOR, false);
    }

    /**
     * Spills the entire stack frame of a method call into typed locals, then
     * reloads them so the original call sees the original stack, and finally
     * boxes the values selected by {@code captureMask} into Object-typed
     * locals for the subsequent {@code recordBranchHit} emission.
     *
     * <p>Handles {@code INVOKESTATIC}, {@code INVOKEVIRTUAL},
     * {@code INVOKEINTERFACE}, and {@code INVOKESPECIAL} with any arity and
     * any mix of category-1 / category-2 argument types.</p>
     *
     * <p>Stack shape after this method returns is identical to the stack on
     * entry, so the original method call sees exactly what it would have seen
     * with no instrumentation.</p>
     *
     * <p>When {@code captureMask} is {@code 0}, returns an empty array
     * immediately — no bytecode is emitted. The {@code pendingOperandLocals ==
     * null} guard in {@code visitMethodInsn} ensures this return value causes
     * {@link #injectBranchProbeCall} to be used instead of the locals form.</p>
     *
     * @param opcode      the {@code INVOKE*} opcode of the call being captured
     * @param descriptor  the JVM method descriptor of the call
     * @param captureMask bitmask: bit 0 = capture receiver (ignored for static
     *                    calls); bit N (N &ge; 1) = capture AST argument N-1
     * @return the Object-typed local slots holding the boxed captured values,
     *         in source order; one entry per set bit in {@code captureMask}
     *         (receiver bit silently ignored for static calls)
     */
    protected int[] captureMethodCallOperand(int opcode, String descriptor, int captureMask) {
        Type[] argTypes = Type.getArgumentTypes(descriptor);
        boolean isStaticCall = opcode == Opcodes.INVOKESTATIC;
        int positionCount = (isStaticCall ? 0 : 1) + argTypes.length;

        if (positionCount == 0 || captureMask == 0) {
            return new int[0];
        }

        // 1. Map each stack position to its JVM Type and a fresh typed local slot.
        //    Position 0 = receiver (instance calls) or arg0 (static calls).
        //    Positions 1..N = args left-to-right following descriptor order.
        Type[] positionTypes = new Type[positionCount];
        int[] spillSlots = new int[positionCount];
        int pos = 0;
        if (!isStaticCall) {
            positionTypes[pos] = OBJECT_TYPE;
            spillSlots[pos] = nextLocalSlot;
            nextLocalSlot += 1;
            pos++;
        }
        for (Type argType : argTypes) {
            positionTypes[pos] = argType;
            spillSlots[pos] = nextLocalSlot;
            nextLocalSlot += argType.getSize();
            pos++;
        }

        // 2. Spill top-to-bottom. TOS is positionCount-1 (last arg pushed last).
        for (int i = positionCount - 1; i >= 0; i--) {
            super.visitVarInsn(storeOpcode(positionTypes[i]), spillSlots[i]);
        }

        // 3. Reload bottom-to-top so the original call sees the original stack.
        for (int i = 0; i < positionCount; i++) {
            super.visitVarInsn(loadOpcode(positionTypes[i]), spillSlots[i]);
        }

        // 4. Box each requested position into a fresh Object-typed local.
        //    injectBranchProbeCallWithLocals builds the Object[] from these slots.
        int firstArgPosition = isStaticCall ? 0 : 1;
        int[] boxedSlots = new int[Integer.bitCount(captureMask)];
        int out = 0;
        int maxBit = argTypes.length; // max meaningful bit index
        for (int bit = 0; bit <= maxBit; bit++) {
            if ((captureMask & (1 << bit)) == 0) {
                continue;
            }
            if (bit == 0 && isStaticCall) {
                // Receiver bit is meaningless for static calls — silently ignore.
                continue;
            }
            int position = (bit == 0) ? 0 : firstArgPosition + (bit - 1);
            Type rawType = positionTypes[position];
            int boxedSlot = nextLocalSlot++;
            super.visitVarInsn(loadOpcode(rawType), spillSlots[position]);
            box(rawType);
            super.visitVarInsn(Opcodes.ASTORE, boxedSlot);
            boxedSlots[out++] = boxedSlot;
        }
        // Trim when the receiver bit was set on a static call (silently ignored).
        return out == boxedSlots.length
                ? boxedSlots
                : java.util.Arrays.copyOf(boxedSlots, out);
    }

    /**
     * Returns the typed {@code *STORE} opcode for {@code type}.
     *
     * @param type the JVM {@link Type} of the value to store
     * @return the matching store opcode (e.g. {@code LSTORE} for {@code long})
     */
    private int storeOpcode(Type type) {
        return type.getOpcode(Opcodes.ISTORE);
    }

    /**
     * Returns the typed {@code *LOAD} opcode for {@code type}.
     *
     * @param type the JVM {@link Type} of the value to load
     * @return the matching load opcode (e.g. {@code DLOAD} for {@code double})
     */
    private int loadOpcode(Type type) {
        return type.getOpcode(Opcodes.ILOAD);
    }

    /**
     * Emits the {@code valueOf} call that boxes the primitive on top of the
     * stack into its wrapper type. No-op for reference types.
     *
     * @param type the JVM {@link Type} of the value on the stack
     */
    private void box(Type type) {
        switch (type.getSort()) {
            case Type.BOOLEAN -> super.visitMethodInsn(Opcodes.INVOKESTATIC,
                    "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
            case Type.BYTE -> super.visitMethodInsn(Opcodes.INVOKESTATIC,
                    "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
            case Type.CHAR -> super.visitMethodInsn(Opcodes.INVOKESTATIC,
                    "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
            case Type.SHORT -> super.visitMethodInsn(Opcodes.INVOKESTATIC,
                    "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
            case Type.INT -> super.visitMethodInsn(Opcodes.INVOKESTATIC,
                    "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
            case Type.LONG -> super.visitMethodInsn(Opcodes.INVOKESTATIC,
                    "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
            case Type.FLOAT -> super.visitMethodInsn(Opcodes.INVOKESTATIC,
                    "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
            case Type.DOUBLE -> super.visitMethodInsn(Opcodes.INVOKESTATIC,
                    "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
            default -> { /* already an Object reference; no boxing needed. */ }
        }
    }

    /**
     * Captures the stack value(s) for a {@code BINARY_COMPARE} operand just
     * before the conditional-jump instruction. Depending on the opcode, the
     * stack holds one value (single-operand {@code IF*} forms) or two values
     * ({@code IF_ICMP*} / {@code IF_ACMP*} forms).
     *
     * <p>The captured values are stored as raw boxed references (primitives are
     * boxed inline via {@code Integer.valueOf} etc.) in fresh JVM local-variable
     * slots. The stack is restored to its original state before this method
     * returns so that the jump instruction executes normally.</p>
     *
     * <p>{@code captureMask} encodes which operands are capturable (non-literal):
     * bit 0 set means the left-hand side is capturable; bit 1 set means the
     * right-hand side is capturable. When both bits are set both values are
     * captured and the returned array has two entries in source order
     * (lhs first). When neither bit is set an empty array is returned.</p>
     *
     * <p><b>Category-2 comparisons ({@code long} / {@code double} / {@code float}).</b>
     * javac emits those as {@code LCMP} / {@code DCMPL} / {@code FCMPL} (or the
     * {@code *G} variants) followed by a single-int {@code IF*}. At the {@code IF*}
     * site the stack holds the comparison result, not the original operands.
     * The captured values for these comparisons therefore come from
     * {@link #captureCategory2ComparisonOperands}, which runs at the preceding
     * {@code CMP} instruction where both operands are still on the stack, and
     * populates {@code pendingOperandLocals} before this method is reached.
     * The {@code default} branch below additionally refuses to capture the
     * single-int stack value when the source model expected two-operand capture
     * — a defensive guard for the case where the CMP-time interceptor did not
     * run (missing / mismatched source model).</p>
     *
     * @param opcode      the conditional-jump opcode
     * @param captureMask bitmask derived from {@link io.github.kd656.coveragex.core.analysis.source.model.OperandModel#binaryCaptureMask()}
     * @return the allocated local-variable slot indices, one per captured value,
     *         or an empty array when {@code captureMask} is 0
     */
    protected int[] captureBinaryComparisonOperand(int opcode, int captureMask) {
        if (captureMask == 0) {
            return new int[0];
        }
        boolean captureLhs = (captureMask & 1) != 0;
        boolean captureRhs = (captureMask & 2) != 0;

        switch (opcode) {
            case Opcodes.IFNULL, Opcodes.IFNONNULL -> {
                // Stack: ..., ref  — one reference; null is a valid value.
                int slot = nextLocalSlot++;
                super.visitInsn(Opcodes.DUP);
                super.visitVarInsn(Opcodes.ASTORE, slot);
                return new int[]{slot};
            }
            case Opcodes.IF_ACMPEQ, Opcodes.IF_ACMPNE -> {
                // Stack: ..., lhs, rhs — two references.
                if (captureLhs && captureRhs) {
                    // Capture lhs (TOS-1) and rhs (TOS).
                    int slotLhs = nextLocalSlot++;
                    int slotRhs = nextLocalSlot++;
                    // Stash rhs (TOS).
                    super.visitInsn(Opcodes.DUP);           // ..., lhs, rhs, rhs
                    super.visitVarInsn(Opcodes.ASTORE, slotRhs); // ..., lhs, rhs
                    // Stash lhs (now TOS-1); swap to reach it.
                    super.visitInsn(Opcodes.SWAP);          // ..., rhs, lhs
                    super.visitInsn(Opcodes.DUP);           // ..., rhs, lhs, lhs
                    super.visitVarInsn(Opcodes.ASTORE, slotLhs); // ..., rhs, lhs
                    super.visitInsn(Opcodes.SWAP);          // ..., lhs, rhs  (restored)
                    return new int[]{slotLhs, slotRhs};
                } else if (captureLhs) {
                    // Capture only lhs (TOS-1).
                    int slot = nextLocalSlot++;
                    super.visitInsn(Opcodes.SWAP);          // ..., rhs, lhs
                    super.visitInsn(Opcodes.DUP);           // ..., rhs, lhs, lhs
                    super.visitVarInsn(Opcodes.ASTORE, slot); // ..., rhs, lhs
                    super.visitInsn(Opcodes.SWAP);          // ..., lhs, rhs  (restored)
                    return new int[]{slot};
                } else {
                    // Capture only rhs (TOS).
                    int slot = nextLocalSlot++;
                    super.visitInsn(Opcodes.DUP);
                    super.visitVarInsn(Opcodes.ASTORE, slot);
                    return new int[]{slot};
                }
            }
            case Opcodes.IF_ICMPEQ, Opcodes.IF_ICMPNE,
                 Opcodes.IF_ICMPLT, Opcodes.IF_ICMPGE,
                 Opcodes.IF_ICMPGT, Opcodes.IF_ICMPLE -> {
                // Stack: ..., lhs (int), rhs (int) — two ints; box before storing.
                if (captureLhs && captureRhs) {
                    // Capture both ints via DUP2 + individual boxing.
                    int slotLhs = nextLocalSlot++;
                    int slotRhs = nextLocalSlot++;
                    // DUP2: ..., lhs, rhs, lhs, rhs
                    super.visitInsn(Opcodes.DUP2);
                    // Top of the duplicates is rhs — box it and store.
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer",
                            "valueOf", "(I)Ljava/lang/Integer;", false);
                    super.visitVarInsn(Opcodes.ASTORE, slotRhs);
                    // Remaining duplicate is lhs — box and store.
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer",
                            "valueOf", "(I)Ljava/lang/Integer;", false);
                    super.visitVarInsn(Opcodes.ASTORE, slotLhs);
                    // Stack restored to: ..., lhs, rhs
                    return new int[]{slotLhs, slotRhs};
                } else if (captureLhs) {
                    // Capture only lhs (TOS-1): SWAP, DUP, box, ASTORE, SWAP.
                    int slot = nextLocalSlot++;
                    super.visitInsn(Opcodes.SWAP);          // ..., rhs, lhs
                    super.visitInsn(Opcodes.DUP);           // ..., rhs, lhs, lhs
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer",
                            "valueOf", "(I)Ljava/lang/Integer;", false);
                    super.visitVarInsn(Opcodes.ASTORE, slot); // ..., rhs, lhs
                    super.visitInsn(Opcodes.SWAP);          // ..., lhs, rhs  (restored)
                    return new int[]{slot};
                } else {
                    // Capture only rhs (TOS): DUP, box, ASTORE.
                    int slot = nextLocalSlot++;
                    super.visitInsn(Opcodes.DUP);
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer",
                            "valueOf", "(I)Ljava/lang/Integer;", false);
                    super.visitVarInsn(Opcodes.ASTORE, slot);
                    return new int[]{slot};
                }
            }
            default -> {
                // Single-operand int forms: IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE.
                // Two source shapes reach here:
                //   (a) `x != 0` / `x == 0` etc. — RHS is a filtered literal,
                //       LHS is the int on the stack. captureMask == 1.
                //   (b) `longA > longB`, `doubleS >= doubleT`, `floatX < floatY`
                //       — the primary capture path for these is at the preceding
                //       CMP instruction via
                //       {@link #captureCategory2ComparisonOperands}, which sets
                //       pendingOperandLocals before this method is reached, so
                //       we normally don't get here. If we do (source model
                //       absent / mismatched), fall back to no capture rather
                //       than grabbing the -1/0/+1 CMP result.
                if (captureMask != 1) {
                    return new int[0];
                }
                int slot = nextLocalSlot++;
                super.visitInsn(Opcodes.DUP);
                super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer",
                        "valueOf", "(I)Ljava/lang/Integer;", false);
                super.visitVarInsn(Opcodes.ASTORE, slot);
                return new int[]{slot};
            }
        }
    }

    /**
     * Captures both operands of a category-2 comparison ({@code LCMP},
     * {@code DCMPL} / {@code DCMPG}, {@code FCMPL} / {@code FCMPG}) at the
     * CMP instruction itself, before the CMP consumes them and reduces the
     * pair to a single {@code int} result on the stack.
     *
     * <p>Emitted sequence for a {@code long/long} comparison (LCMP) with
     * {@code captureMask == 3}:</p>
     * <pre>
     *   Stack on entry: ..., lhs (long), rhs (long)
     *
     *   LSTORE  slotRhsLong          ; consumes rhs; ..., lhs
     *   LSTORE  slotLhsLong          ; consumes lhs; ...
     *   LLOAD   slotLhsLong          ; ..., lhs
     *   INVOKESTATIC Long.valueOf    ; ..., Long
     *   ASTORE  slotLhsBoxed         ; ...
     *   LLOAD   slotRhsLong          ; ..., rhs
     *   INVOKESTATIC Long.valueOf    ; ..., Long
     *   ASTORE  slotRhsBoxed         ; ...
     *   LLOAD   slotLhsLong          ; ..., lhs
     *   LLOAD   slotRhsLong          ; ..., lhs, rhs   (restored for CMP)
     *
     *   The caller then emits LCMP + the original IF*.
     * </pre>
     *
     * <p>{@code DCMPL/DCMPG} use the {@code D*} instructions and
     * {@code Double.valueOf}; {@code FCMPL/FCMPG} use the {@code F*}
     * instructions and {@code Float.valueOf}. Long and double take two JVM
     * local slots; float takes one — {@link #nextLocalSlot} is advanced
     * accordingly so future allocations don't clobber the raw-value slots.</p>
     *
     * <p><b>Only {@code captureMask == 3} is meaningful.</b> The analyser sets
     * mask to {@code 0} when either operand is a literal (in which case this
     * method is not called); asymmetric masks ({@code 1} or {@code 2}) cannot
     * arise for var-vs-var category-2 comparisons in practice, so this
     * method returns an empty array for them defensively rather than emitting
     * a partial-capture sequence.</p>
     *
     * @param cmpOpcode   the CMP opcode ({@code LCMP} / {@code DCMPL} /
     *                    {@code DCMPG} / {@code FCMPL} / {@code FCMPG})
     * @param captureMask capture mask from the source model
     * @return the two boxed-value slot indices in {@code [lhs, rhs]} order,
     *         or an empty array when capture is not performed
     */
    protected int[] captureCategory2ComparisonOperands(int cmpOpcode, int captureMask) {
        if (captureMask != 0b11) {
            return new int[0];
        }
        String boxOwner;
        String boxDesc;
        int loadOp;
        int storeOp;
        int rawSlotSize;
        switch (cmpOpcode) {
            case Opcodes.LCMP -> {
                boxOwner = "java/lang/Long";
                boxDesc  = "(J)Ljava/lang/Long;";
                loadOp   = Opcodes.LLOAD;
                storeOp  = Opcodes.LSTORE;
                rawSlotSize = 2;
            }
            case Opcodes.DCMPL, Opcodes.DCMPG -> {
                boxOwner = "java/lang/Double";
                boxDesc  = "(D)Ljava/lang/Double;";
                loadOp   = Opcodes.DLOAD;
                storeOp  = Opcodes.DSTORE;
                rawSlotSize = 2;
            }
            case Opcodes.FCMPL, Opcodes.FCMPG -> {
                boxOwner = "java/lang/Float";
                boxDesc  = "(F)Ljava/lang/Float;";
                loadOp   = Opcodes.FLOAD;
                storeOp  = Opcodes.FSTORE;
                rawSlotSize = 1;
            }
            default -> {
                return new int[0];
            }
        }

        int slotLhsRaw = nextLocalSlot;
        nextLocalSlot += rawSlotSize;
        int slotRhsRaw = nextLocalSlot;
        nextLocalSlot += rawSlotSize;
        int slotLhsBoxed = nextLocalSlot++;
        int slotRhsBoxed = nextLocalSlot++;

        // Stack: ..., lhs, rhs → store rhs then lhs.
        super.visitVarInsn(storeOp, slotRhsRaw);
        super.visitVarInsn(storeOp, slotLhsRaw);

        // Box lhs into slotLhsBoxed.
        super.visitVarInsn(loadOp, slotLhsRaw);
        super.visitMethodInsn(Opcodes.INVOKESTATIC, boxOwner, "valueOf", boxDesc, false);
        super.visitVarInsn(Opcodes.ASTORE, slotLhsBoxed);

        // Box rhs into slotRhsBoxed.
        super.visitVarInsn(loadOp, slotRhsRaw);
        super.visitMethodInsn(Opcodes.INVOKESTATIC, boxOwner, "valueOf", boxDesc, false);
        super.visitVarInsn(Opcodes.ASTORE, slotRhsBoxed);

        // Restore stack for the CMP that follows.
        super.visitVarInsn(loadOp, slotLhsRaw);
        super.visitVarInsn(loadOp, slotRhsRaw);

        return new int[]{slotLhsBoxed, slotRhsBoxed};
    }

    /**
     * Captures the single boolean value sitting on top of the operand stack
     * before an {@code IFEQ} / {@code IFNE} jump, boxing it as
     * {@link Boolean} into a fresh local slot. Used for
     * {@link io.github.kd656.coveragex.api.data.OperandKind#BARE_REFERENCE}
     * operands (e.g. {@code if (flag) { ... }} or the test expression of a
     * boolean ternary) and for {@link io.github.kd656.coveragex.api.data.OperandKind#UNARY}
     * operands, where the JVM has already reduced the operand to a single
     * {@code int}-typed boolean on the stack.
     *
     * <p>Stack on entry: {@code ..., value (int)}. Stack on exit:
     * {@code ..., value (int)} — the original value is preserved so the
     * conditional jump executes normally; the boxed copy lives in the
     * returned local slot.</p>
     *
     * @param opcode the conditional-jump opcode; must be {@code IFEQ} or
     *               {@code IFNE}
     * @return a one-element array holding the allocated local slot, or
     *         an empty array when {@code opcode} is not a single-int
     *         conditional jump
     */
    protected int[] captureBooleanStackValue(int opcode) {
        if (opcode != Opcodes.IFEQ && opcode != Opcodes.IFNE) {
            return new int[0];
        }
        int slot = nextLocalSlot++;
        super.visitInsn(Opcodes.DUP);
        super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean",
                "valueOf", "(Z)Ljava/lang/Boolean;", false);
        super.visitVarInsn(Opcodes.ASTORE, slot);
        return new int[]{slot};
    }

    /**
     * Builds the boxed argument array for method-entry probes. The array is
     * left on the operand stack in the position expected by
     * {@code recordMethodEntry}.
     */
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
            // JVM local slots are descriptor-shaped: long/double consume two
            // slots, references one.
            loadAndBoxArgument(argType, slot);
            super.visitInsn(Opcodes.AASTORE);
            slot += argType.getSize();
        }
    }

    /**
     * Emits a load instruction for the argument at {@code slot}, followed by
     * the appropriate boxing method call so that primitive values are promoted
     * to their wrapper types before being stored in the {@code Object[]} args
     * array.
     *
     * @param type the ASM {@link Type} of the argument
     * @param slot the JVM local-variable slot index
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

    /**
     * Returns a human-readable condition text for the given conditional-jump
     * opcode, delegating to {@link ProbeOpcodeSupport}.
     *
     * @param opcode the conditional-jump opcode
     * @return a descriptive string for the opcode
     */
    protected String opcodeToConditionText(int opcode) {
        return ProbeOpcodeSupport.opcodeToConditionText(opcode);
    }
}

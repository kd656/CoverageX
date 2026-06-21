package com.coveragex.core.probe;

import org.objectweb.asm.Opcodes;

/**
 * Bytecode opcode helpers shared by runtime instrumentation and static probe planning.
 *
 * <p>Keeping this table in core prevents drift between the agent's emitted probes and
 * enrichment's zero-coverage metadata. Any change to branch support or fallback labels
 * should happen here first.</p>
 */
public final class ProbeOpcodeSupport {

    private ProbeOpcodeSupport() {
    }

    public static boolean isBranchInstruction(int opcode) {
        return opcode == Opcodes.IFEQ || opcode == Opcodes.IFNE
                || opcode == Opcodes.IFLT || opcode == Opcodes.IFGE
                || opcode == Opcodes.IFGT || opcode == Opcodes.IFLE
                || opcode == Opcodes.IF_ICMPEQ || opcode == Opcodes.IF_ICMPNE
                || opcode == Opcodes.IF_ICMPLT || opcode == Opcodes.IF_ICMPGE
                || opcode == Opcodes.IF_ICMPGT || opcode == Opcodes.IF_ICMPLE
                || opcode == Opcodes.IF_ACMPEQ || opcode == Opcodes.IF_ACMPNE
                || opcode == Opcodes.IFNULL || opcode == Opcodes.IFNONNULL;
    }

    public static boolean isReturn(int opcode) {
        return opcode == Opcodes.IRETURN || opcode == Opcodes.LRETURN
                || opcode == Opcodes.FRETURN || opcode == Opcodes.DRETURN
                || opcode == Opcodes.ARETURN || opcode == Opcodes.RETURN;
    }

    public static boolean isThrow(int opcode) {
        return opcode == Opcodes.ATHROW;
    }

    public static boolean isJumpTakenWhenTrue(int opcode) {
        return switch (opcode) {
            case Opcodes.IFNE, Opcodes.IFGT, Opcodes.IFGE, Opcodes.IFNONNULL, Opcodes.IFNULL,
                 Opcodes.IF_ICMPNE, Opcodes.IF_ICMPGT, Opcodes.IF_ICMPGE, Opcodes.IF_ACMPNE -> true;
            default -> false;
        };
    }

    public static String opcodeToConditionText(int opcode) {
        return switch (opcode) {
            case Opcodes.IFEQ -> "if (x == 0)";
            case Opcodes.IFNE -> "if (x != 0)";
            case Opcodes.IFLT -> "if (x < 0)";
            case Opcodes.IFGE -> "if (x >= 0)";
            case Opcodes.IFGT -> "if (x > 0)";
            case Opcodes.IFLE -> "if (x <= 0)";
            case Opcodes.IF_ICMPEQ, Opcodes.IF_ACMPEQ -> "if (a == b)";
            case Opcodes.IF_ICMPNE, Opcodes.IF_ACMPNE -> "if (a != b)";
            case Opcodes.IF_ICMPLT -> "if (a < b)";
            case Opcodes.IF_ICMPGE -> "if (a >= b)";
            case Opcodes.IF_ICMPGT -> "if (a > b)";
            case Opcodes.IF_ICMPLE -> "if (a <= b)";
            case Opcodes.IFNULL -> "if (x == null)";
            case Opcodes.IFNONNULL -> "if (x != null)";
            default -> "if (condition)";
        };
    }
}

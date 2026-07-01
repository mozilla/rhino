package org.mozilla.javascript.interpreterv2;

import java.math.BigInteger;
import java.util.Arrays;
import org.mozilla.javascript.interpreterv2.instruction.Instruction;
import org.mozilla.javascript.interpreterv2.operand.Operand;

/** Utility class for formatting InterpreterV2 instructions. */
public class InstructionFormatter {

    /**
     * Format a jump target as a virtual register reference.
     *
     * @param targetPC the target program counter
     * @return formatted target reference (e.g., "%7", "%12")
     */
    public static String formatTarget(int targetPC) {
        return "%" + targetPC;
    }

    /**
     * Format a relative jump offset for display.
     *
     * @param offset the jump offset (positive or negative)
     * @return formatted offset string (e.g., "+5", "-3")
     */
    public static String formatOffset(int offset) {
        return (offset >= 0 ? "+" : "") + offset;
    }

    /**
     * Format multiple operands as a comma-separated parameter list.
     *
     * @param operands variable number of key-value pairs (key1, value1, key2, value2, ...)
     * @return formatted parameter list (e.g., "args=2, receiver=true")
     */
    public static String formatInstruction(Instruction instruction, Object... operands) {
        return formatInstruction(instruction.getClass().getSimpleName(), operands);
    }

    /**
     * Like {@link #formatInstruction(Instruction, Object...)} but with an explicit instruction
     * name. Used by specialized variants (e.g. the arity-specialized {@code Call}) that want to
     * present themselves under a common name regardless of their concrete class.
     */
    public static String formatInstruction(String name, Object... operands) {
        StringBuilder sb = new StringBuilder();
        sb.append(name);

        if (operands.length != 0) {
            if (operands.length % 2 != 0) {
                throw new IllegalArgumentException("Operands must be provided in key-value pairs");
            }

            sb.append('(');
            for (int i = 0; i < operands.length; i += 2) {
                if (i > 0) {
                    sb.append(", ");
                }

                String key = operands[i].toString();
                Object value = operands[i + 1];

                sb.append(key).append('=');
                formatOperandValue(sb, value);
            }
            sb.append(')');
        }

        return sb.toString();
    }

    private static void formatOperandValue(StringBuilder sb, Object value) {
        if (value instanceof Object[]) {
            sb.append('[');
            Object[] operands = (Object[]) value;
            for (int i = 0; i < operands.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                formatOperandValue(sb, operands[i]);
            }
            sb.append(']');
        } else if (value instanceof boolean[]) {
            sb.append(Arrays.toString((boolean[]) value));
        } else if (value instanceof Operand) {
            ((Operand) value).appendDebugString(sb);
        } else if (value instanceof String) {
            appendString(sb, (String) value);
        } else if (value instanceof BigInteger) {
            sb.append(value).append('n');
        } else {
            sb.append(value);
        }
    }

    public static void appendString(StringBuilder sb, String value) {
        sb.append('"').append(escape(value)).append('"');
    }

    private static String escape(String str) {
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\t", "\\t")
                .replace("\r", "\\r");
    }
}

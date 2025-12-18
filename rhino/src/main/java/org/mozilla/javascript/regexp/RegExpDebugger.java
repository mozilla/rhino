/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.regexp;

/**
 * Debug utilities for regex compilation and execution.
 *
 * <p>Controlled by the {@code NativeRegExp.debug} compile-time constant. When enabled, outputs
 * detailed bytecode disassembly to System.out for debugging pattern compilation.
 */
public final class RegExpDebugger {

    private RegExpDebugger() {
        // Utility class - no instances
    }

    /**
     * Log the compilation of a regular expression pattern.
     *
     * @param pattern the original regex pattern
     * @param flags compilation flags
     * @param regexp the compiled regex
     */
    public static void logCompilation(String pattern, int flags, RECompiled regexp) {
        if (!NativeRegExp.debug) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n=== RegExp Compilation ===\n");
        sb.append("Pattern: ").append(pattern).append("\n");
        sb.append("Flags: ").append(formatFlags(flags)).append("\n");
        sb.append("Program length: ").append(regexp.program.length).append(" bytes\n");

        // Dump raw bytecode
        sb.append("Raw bytecode: [");
        for (int i = 0; i < regexp.program.length && i < 100; i++) {
            sb.append(regexp.program[i] & 0xFF);
            if (i < regexp.program.length - 1) {
                sb.append(", ");
            }
        }
        if (regexp.program.length > 100) {
            sb.append("... (truncated)");
        }
        sb.append("]\n\n");

        // Disassemble bytecode
        logBytecode(regexp, sb);

        System.out.println(sb.toString());
    }

    /**
     * Disassemble the compiled bytecode into human-readable instructions.
     *
     * @param regexp the compiled regex
     * @param sb string builder to append output
     */
    private static void logBytecode(RECompiled regexp, StringBuilder sb) {
        sb.append("=== Bytecode Disassembly ===\n");

        byte[] program = regexp.program;
        int pc = 0;

        while (pc < program.length && program[pc] != NativeRegExp.REOP_END) {
            sb.append(String.format("%4d: ", pc));
            byte op = program[pc];
            pc++;

            switch (op) {
                case NativeRegExp.REOP_EMPTY:
                    sb.append("EMPTY\n");
                    break;

                case NativeRegExp.REOP_BOL:
                    sb.append("BOL (^)\n");
                    break;

                case NativeRegExp.REOP_EOL:
                    sb.append("EOL ($)\n");
                    break;

                case NativeRegExp.REOP_WBDRY:
                    sb.append("WBDRY (\\b)\n");
                    break;

                case NativeRegExp.REOP_WNONBDRY:
                    sb.append("WNONBDRY (\\B)\n");
                    break;

                case NativeRegExp.REOP_DOT:
                    sb.append("DOT (.)\n");
                    break;

                case NativeRegExp.REOP_DIGIT:
                    sb.append("DIGIT (\\d)\n");
                    break;

                case NativeRegExp.REOP_NONDIGIT:
                    sb.append("NONDIGIT (\\D)\n");
                    break;

                case NativeRegExp.REOP_ALNUM:
                    sb.append("ALNUM (\\w)\n");
                    break;

                case NativeRegExp.REOP_NONALNUM:
                    sb.append("NONALNUM (\\W)\n");
                    break;

                case NativeRegExp.REOP_SPACE:
                    sb.append("SPACE (\\s)\n");
                    break;

                case NativeRegExp.REOP_NONSPACE:
                    sb.append("NONSPACE (\\S)\n");
                    break;

                case NativeRegExp.REOP_BACKREF:
                    {
                        int index = readIndex(program, pc);
                        pc += NativeRegExp.INDEX_LEN;
                        sb.append("BACKREF #").append(index).append("\n");
                    }
                    break;

                case NativeRegExp.REOP_FLAT:
                    {
                        int offset = readIndex(program, pc);
                        pc += NativeRegExp.INDEX_LEN;
                        int length = readIndex(program, pc);
                        pc += NativeRegExp.INDEX_LEN;
                        sb.append("FLAT offset=").append(offset).append(" length=").append(length);
                        sb.append("\n");
                    }
                    break;

                case NativeRegExp.REOP_FLAT1:
                    {
                        char ch = (char) readIndex(program, pc);
                        pc += NativeRegExp.INDEX_LEN;
                        sb.append("FLAT1 '").append(printableChar(ch)).append("'\n");
                    }
                    break;

                case NativeRegExp.REOP_FLATi:
                    {
                        int offset = readIndex(program, pc);
                        pc += NativeRegExp.INDEX_LEN;
                        int length = readIndex(program, pc);
                        pc += NativeRegExp.INDEX_LEN;
                        sb.append("FLATi offset=").append(offset).append(" length=").append(length);
                        sb.append("\n");
                    }
                    break;

                case NativeRegExp.REOP_FLAT1i:
                    {
                        char ch = (char) readIndex(program, pc);
                        pc += NativeRegExp.INDEX_LEN;
                        sb.append("FLAT1i '").append(printableChar(ch)).append("'\n");
                    }
                    break;

                case NativeRegExp.REOP_UCFLAT1:
                    {
                        char ch = (char) readIndex(program, pc);
                        pc += NativeRegExp.INDEX_LEN;
                        sb.append("UCFLAT1 '").append(printableChar(ch)).append("'\n");
                    }
                    break;

                case NativeRegExp.REOP_UCFLAT1i:
                    {
                        char ch = (char) readIndex(program, pc);
                        pc += NativeRegExp.INDEX_LEN;
                        sb.append("UCFLAT1i '").append(printableChar(ch)).append("'\n");
                    }
                    break;

                case NativeRegExp.REOP_CLASS:
                    {
                        int index = readIndex(program, pc);
                        pc += NativeRegExp.INDEX_LEN;
                        sb.append("CLASS #").append(index).append("\n");
                    }
                    break;

                case NativeRegExp.REOP_NCLASS:
                    {
                        int index = readIndex(program, pc);
                        pc += NativeRegExp.INDEX_LEN;
                        sb.append("NCLASS #").append(index).append("\n");
                    }
                    break;

                case NativeRegExp.REOP_ALT:
                    {
                        int offset = readIndex(program, pc);
                        pc += NativeRegExp.INDEX_LEN;
                        sb.append("ALT next=").append(pc + offset).append("\n");
                    }
                    break;

                case NativeRegExp.REOP_QUANT:
                    {
                        int min = readIndex(program, pc);
                        pc += NativeRegExp.INDEX_LEN;
                        int max = readIndex(program, pc);
                        pc += NativeRegExp.INDEX_LEN;
                        int offset = readIndex(program, pc);
                        pc += NativeRegExp.INDEX_LEN;
                        int parenIndex = readIndex(program, pc);
                        pc += NativeRegExp.INDEX_LEN;
                        int parenCount = readIndex(program, pc);
                        pc += NativeRegExp.INDEX_LEN;
                        sb.append("QUANT {").append(min).append(",");
                        if (max == -1) {
                            sb.append("inf");
                        } else {
                            sb.append(max);
                        }
                        sb.append("} next=").append(pc + offset);
                        sb.append(" paren=").append(parenIndex);
                        sb.append(" count=").append(parenCount).append("\n");
                    }
                    break;

                case NativeRegExp.REOP_STAR:
                    sb.append("STAR (*)\n");
                    break;

                case NativeRegExp.REOP_PLUS:
                    sb.append("PLUS (+)\n");
                    break;

                case NativeRegExp.REOP_OPT:
                    sb.append("OPT (?)\n");
                    break;

                case NativeRegExp.REOP_LPAREN:
                    {
                        int index = readIndex(program, pc);
                        pc += NativeRegExp.INDEX_LEN;
                        int min = readIndex(program, pc);
                        pc += NativeRegExp.INDEX_LEN;
                        int max = readIndex(program, pc);
                        pc += NativeRegExp.INDEX_LEN;
                        sb.append("LPAREN #").append(index);
                        sb.append(" {").append(min).append(",").append(max).append("}\n");
                    }
                    break;

                case NativeRegExp.REOP_RPAREN:
                    {
                        int index = readIndex(program, pc);
                        pc += NativeRegExp.INDEX_LEN;
                        sb.append("RPAREN #").append(index).append("\n");
                    }
                    break;

                case NativeRegExp.REOP_JUMP:
                    {
                        int offset = readIndex(program, pc);
                        pc += NativeRegExp.INDEX_LEN;
                        sb.append("JUMP to ").append(pc + offset).append("\n");
                    }
                    break;

                case NativeRegExp.REOP_MINIMALSTAR:
                    sb.append("MINIMALSTAR (*?)\n");
                    break;

                case NativeRegExp.REOP_MINIMALPLUS:
                    sb.append("MINIMALPLUS (+?)\n");
                    break;

                case NativeRegExp.REOP_MINIMALOPT:
                    sb.append("MINIMALOPT (??)\n");
                    break;

                case NativeRegExp.REOP_MINIMALQUANT:
                    {
                        int min = readIndex(program, pc);
                        pc += NativeRegExp.INDEX_LEN;
                        int max = readIndex(program, pc);
                        pc += NativeRegExp.INDEX_LEN;
                        int offset = readIndex(program, pc);
                        pc += NativeRegExp.INDEX_LEN;
                        int parenIndex = readIndex(program, pc);
                        pc += NativeRegExp.INDEX_LEN;
                        int parenCount = readIndex(program, pc);
                        pc += NativeRegExp.INDEX_LEN;
                        sb.append("MINIMALQUANT {").append(min).append(",");
                        if (max == -1) {
                            sb.append("inf");
                        } else {
                            sb.append(max);
                        }
                        sb.append("}? next=").append(pc + offset);
                        sb.append(" paren=").append(parenIndex);
                        sb.append(" count=").append(parenCount).append("\n");
                    }
                    break;

                case NativeRegExp.REOP_ENDCHILD:
                    sb.append("ENDCHILD\n");
                    break;

                case NativeRegExp.REOP_REPEAT:
                    {
                        int offset = readIndex(program, pc);
                        pc += NativeRegExp.INDEX_LEN;
                        sb.append("REPEAT to ").append(pc + offset).append("\n");
                    }
                    break;

                case NativeRegExp.REOP_MINIMALREPEAT:
                    {
                        int offset = readIndex(program, pc);
                        pc += NativeRegExp.INDEX_LEN;
                        sb.append("MINIMALREPEAT to ").append(pc + offset).append("\n");
                    }
                    break;

                case NativeRegExp.REOP_ALTPREREQ:
                    {
                        char ch = (char) readIndex(program, pc);
                        pc += NativeRegExp.INDEX_LEN;
                        int offset = readIndex(program, pc);
                        pc += NativeRegExp.INDEX_LEN;
                        sb.append("ALTPREREQ '").append(printableChar(ch));
                        sb.append("' next=").append(pc + offset).append("\n");
                    }
                    break;

                case NativeRegExp.REOP_ALTPREREQ2:
                    {
                        char ch1 = (char) readIndex(program, pc);
                        pc += NativeRegExp.INDEX_LEN;
                        char ch2 = (char) readIndex(program, pc);
                        pc += NativeRegExp.INDEX_LEN;
                        int offset = readIndex(program, pc);
                        pc += NativeRegExp.INDEX_LEN;
                        sb.append("ALTPREREQ2 '").append(printableChar(ch1));
                        sb.append("' '").append(printableChar(ch2));
                        sb.append("' next=").append(pc + offset).append("\n");
                    }
                    break;

                case NativeRegExp.REOP_ASSERTTEST:
                    {
                        int offset = readIndex(program, pc);
                        pc += NativeRegExp.INDEX_LEN;
                        sb.append("ASSERTTEST next=").append(pc + offset).append("\n");
                    }
                    break;

                case NativeRegExp.REOP_ASSERTNOTTEST:
                    {
                        int offset = readIndex(program, pc);
                        pc += NativeRegExp.INDEX_LEN;
                        sb.append("ASSERTNOTTEST next=").append(pc + offset).append("\n");
                    }
                    break;

                case NativeRegExp.REOP_ASSERT:
                    {
                        int offset = readIndex(program, pc);
                        pc += NativeRegExp.INDEX_LEN;
                        sb.append("ASSERT next=").append(pc + offset).append("\n");
                    }
                    break;

                case NativeRegExp.REOP_ASSERT_NOT:
                    {
                        int offset = readIndex(program, pc);
                        pc += NativeRegExp.INDEX_LEN;
                        sb.append("ASSERT_NOT next=").append(pc + offset).append("\n");
                    }
                    break;

                case NativeRegExp.REOP_UPROP:
                    {
                        int encodedProp = readIndex(program, pc);
                        pc += NativeRegExp.INDEX_LEN;
                        sb.append("UPROP prop=").append(encodedProp).append("\n");
                    }
                    break;

                case NativeRegExp.REOP_UPROP_NOT:
                    {
                        int encodedProp = readIndex(program, pc);
                        pc += NativeRegExp.INDEX_LEN;
                        sb.append("UPROP_NOT prop=").append(encodedProp).append("\n");
                    }
                    break;

                case NativeRegExp.REOP_ASSERTBACK:
                    {
                        int offset = readIndex(program, pc);
                        pc += NativeRegExp.INDEX_LEN;
                        sb.append("ASSERTBACK next=").append(pc + offset).append("\n");
                    }
                    break;

                case NativeRegExp.REOP_ASSERTBACK_NOT:
                    {
                        int offset = readIndex(program, pc);
                        pc += NativeRegExp.INDEX_LEN;
                        sb.append("ASSERTBACK_NOT next=").append(pc + offset).append("\n");
                    }
                    break;

                case NativeRegExp.REOP_ASSERTBACKTEST:
                    {
                        int offset = readIndex(program, pc);
                        pc += NativeRegExp.INDEX_LEN;
                        sb.append("ASSERTBACKTEST next=").append(pc + offset).append("\n");
                    }
                    break;

                case NativeRegExp.REOP_ASSERTBACKNOTTEST:
                    {
                        int offset = readIndex(program, pc);
                        pc += NativeRegExp.INDEX_LEN;
                        sb.append("ASSERTBACKNOTTEST next=").append(pc + offset).append("\n");
                    }
                    break;

                default:
                    sb.append("UNKNOWN_OP(").append(op & 0xFF).append(")\n");
                    break;
            }
        }

        if (pc < program.length && program[pc] == NativeRegExp.REOP_END) {
            sb.append(String.format("%4d: ", pc));
            sb.append("END\n");
        }
    }

    /**
     * Read a 2-byte (16-bit) index from the program bytecode.
     *
     * @param program the bytecode array
     * @param pc program counter
     * @return the decoded index value
     */
    private static int readIndex(byte[] program, int pc) {
        return ((program[pc] & 0xFF) << 8) | (program[pc + 1] & 0xFF);
    }

    /**
     * Format a character for display in debug output.
     *
     * @param ch the character to format
     * @return printable representation
     */
    private static String printableChar(char ch) {
        if (ch >= 32 && ch < 127) {
            return String.valueOf(ch);
        }
        return String.format("\\u%04x", (int) ch);
    }

    /**
     * Format regex compilation flags as a readable string.
     *
     * @param flags the flags bitmask
     * @return formatted flags string
     */
    private static String formatFlags(int flags) {
        StringBuilder sb = new StringBuilder();
        if ((flags & NativeRegExp.JSREG_GLOB) != 0) {
            sb.append("g");
        }
        if ((flags & NativeRegExp.JSREG_FOLD) != 0) {
            sb.append("i");
        }
        if ((flags & NativeRegExp.JSREG_MULTILINE) != 0) {
            sb.append("m");
        }
        if (sb.length() == 0) {
            return "(none)";
        }
        return sb.toString();
    }
}

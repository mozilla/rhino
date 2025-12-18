/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.regexp;

import org.mozilla.javascript.Context;

/**
 * Execution context for regex bytecode operations. Encapsulates state needed for instruction
 * execution.
 */
public final class ExecutionContext {
    /** Result of executing a regex bytecode operation. */
    static final class Result {
        private final boolean matched;
        private final int nextPc; // program counter for next instruction

        private Result(boolean matched, int nextPc) {
            this.matched = matched;
            this.nextPc = nextPc;
        }

        /**
         * Creates a successful match result.
         *
         * @param nextPc program counter for the next instruction to execute
         * @return successful execution result
         */
        static Result success(int nextPc) {
            return new Result(true, nextPc);
        }

        /**
         * Creates a failed match result.
         *
         * @return failed execution result
         */
        static Result failure() {
            return new Result(false, -1);
        }

        /**
         * @return true if the operation matched successfully
         */
        boolean matched() {
            return matched;
        }

        /**
         * @return program counter for next instruction (only valid if matched)
         */
        int nextPc() {
            return nextPc;
        }

        @Override
        public String toString() {
            return matched
                    ? "ExecutionContext.Result{matched, nextPc=" + nextPc + "}"
                    : "ExecutionContext.Result{failed}";
        }
    }

    // Input and bounds
    public final String input;
    public final int end;

    // Execution state
    public final REGlobalData gData;
    public final byte[] program;
    public final Context cx;
    public final boolean matchBackward;

    // Mutable program counter
    public int pc;

    ExecutionContext(
            Context cx,
            REGlobalData gData,
            String input,
            int end,
            byte[] program,
            int initialPc,
            boolean matchBackward) {
        this.cx = cx;
        this.gData = gData;
        this.input = input;
        this.end = end;
        this.program = program;
        this.pc = initialPc;
        this.matchBackward = matchBackward;
    }

    /**
     * @return true if regex is in Unicode mode (/u flag)
     */
    public boolean isUnicodeMode() {
        return (gData.regexp.flags & NativeRegExp.JSREG_UNICODE) != 0;
    }

    /**
     * @return true if regex is in Unicode Sets mode (/v flag)
     */
    public boolean isUnicodeSetsMode() {
        return (gData.regexp.flags & NativeRegExp.JSREG_UNICODESETS) != 0;
    }

    /**
     * @return true if regex is multiline mode (/m flag)
     */
    public boolean isMultiline() {
        return gData.multiline;
    }

    /**
     * @return true if regex is case-insensitive (/i flag)
     */
    public boolean isCaseInsensitive() {
        return (gData.regexp.flags & NativeRegExp.JSREG_FOLD) != 0;
    }

    /**
     * @return true if regex is in dotAll mode (/s flag - dot matches line terminators)
     */
    public boolean isDotAll() {
        return (gData.regexp.flags & NativeRegExp.JSREG_DOTALL) != 0;
    }

    /**
     * @return true if current position is within input bounds
     */
    public boolean isInBounds() {
        return gData.cp >= 0 && gData.cp < end;
    }

    /**
     * Get the position to check for character matching. Accounts for surrogate pairs in Unicode
     * mode.
     *
     * @return the position to check
     */
    public int getMatchPosition() {
        if (isUnicodeMode() && gData.cp < end) {
            if (matchBackward) {
                // Ensure we have at least one character to match
                if (gData.cp < 1) {
                    return -1; // Out of bounds
                }
                // Check for surrogate pair
                if (gData.cp >= 2
                        && Character.isSurrogatePair(
                                input.charAt(gData.cp - 2), input.charAt(gData.cp - 1))) {
                    return gData.cp - 2;
                } else {
                    return gData.cp - 1;
                }
            } else {
                return gData.cp;
            }
        } else {
            int pos = gData.cp + (matchBackward ? -1 : 0);
            if (pos < 0) return -1; // Out of bounds
            return pos;
        }
    }

    /**
     * Get the number of characters to advance if a match succeeds. Accounts for surrogate pairs.
     *
     * @return the number of characters to advance (can be negative)
     */
    public int getMatchDelta() {
        if (isUnicodeMode() && gData.cp < end) {
            if (matchBackward) {
                if (gData.cp - 2 >= 0
                        && Character.isSurrogatePair(
                                input.charAt(gData.cp - 2), input.charAt(gData.cp - 1))) {
                    return -2;
                } else {
                    return -1;
                }
            } else {
                return Character.charCount(input.codePointAt(gData.cp));
            }
        } else {
            return matchBackward ? -1 : 1;
        }
    }

    /**
     * Check if the match position is within bounds.
     *
     * @return true if getMatchPosition() is valid
     */
    public boolean isMatchPositionInBounds() {
        int matchPos = getMatchPosition();
        return matchPos >= 0 && matchPos < end;
    }

    /**
     * Get current match position.
     *
     * @return current position in input string
     */
    public int getCurrentPosition() {
        return gData.cp;
    }

    /**
     * Get character at current position.
     *
     * @return character at gData.cp, or -1 if out of bounds
     */
    public int getCurrentChar() {
        return isInBounds() ? input.charAt(gData.cp) : -1;
    }

    /**
     * Get codepoint at current position (handles surrogate pairs).
     *
     * @return codepoint at gData.cp, or -1 if out of bounds
     */
    public int getCurrentCodePoint() {
        return isInBounds() ? input.codePointAt(gData.cp) : -1;
    }

    /**
     * Advance current position by delta characters.
     *
     * @param delta number of characters to advance (can be negative for backward)
     */
    public void advancePosition(int delta) {
        gData.cp += delta;
    }

    /**
     * Read an index value from the program at current PC and advance PC.
     *
     * @return the index value
     */
    public int readIndex() {
        int index = NativeRegExp.getIndex(program, pc);
        pc += NativeRegExp.INDEX_LEN;
        return index;
    }

    /**
     * Read a byte from the program at current PC and advance PC.
     *
     * @return the byte value
     */
    public byte readByte() {
        return program[pc++];
    }

    /**
     * Try to match a string literal from the character class.
     *
     * @param classIndex the character class index
     * @param position position in input to match from
     * @return length of matched string literal, or -1 if no match
     */
    public int matchStringLiteral(int classIndex, int position) {
        return NativeRegExp.stringLiteralMatcher(
                gData.regexp.classList[classIndex], input, position, matchBackward);
    }

    /**
     * Check if a codepoint matches the character class.
     *
     * @param classIndex the character class index
     * @param codePoint the codepoint to check
     * @return true if the codepoint matches the class
     */
    public boolean matchesCharacterClass(int classIndex, int codePoint) {
        return NativeRegExp.classMatcher(gData, gData.regexp.classList[classIndex], codePoint);
    }
}

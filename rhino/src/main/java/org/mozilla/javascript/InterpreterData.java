/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.mozilla.javascript.sourcemap.Position;

final class InterpreterData<T extends ScriptOrFn<T>> extends ACompilerData<T, InterpreterData<?>>
        implements Serializable {
    @Serial private static final long serialVersionUID = 5067677351589230234L;

    static final int INITIAL_MAX_ICODE_LENGTH = 1024;
    static final int INITIAL_STRINGTABLE_SIZE = 64;
    static final int INITIAL_NUMBERTABLE_SIZE = 64;
    static final int INITIAL_BIGINTTABLE_SIZE = 64;
    static final int INITIAL_POSITION_TABLE_SIZE = 64;

    InterpreterData(
            String[] itsStringTable,
            double[] itsDoubleTable,
            BigInteger[] itsBigIntTable,
            Object[] itsRegExpLiterals,
            Object[] itsTemplateLiterals,
            byte[] itsICode,
            int[] exceptionTable,
            int maxVars,
            int maxLocals,
            int maxStack,
            int maxFrameArray,
            int maxCalleeArgs,
            Object[] literalIds,
            Map<Integer, Integer> longJumps,
            int firstLineOperandPC,
            int[] sourcePositions,
            short[] positionSourceIndexes,
            String[] positionSourceNames) {
        super(maxVars, maxLocals, maxStack, maxFrameArray, exceptionTable);
        this.sourcePositions = sourcePositions;
        this.positionSourceIndexes = positionSourceIndexes;
        this.positionSourceNames = positionSourceNames;
        this.itsStringTable = itsStringTable;
        this.itsDoubleTable = itsDoubleTable;
        this.itsBigIntTable = itsBigIntTable;
        this.itsRegExpLiterals = itsRegExpLiterals;
        this.itsTemplateLiterals = itsTemplateLiterals;
        this.itsICode = itsICode;
        this.maxCalleeArgs = maxCalleeArgs;
        this.literalIds = literalIds;
        this.longJumps = longJumps;
        this.firstLineOperandPC = firstLineOperandPC;
    }

    final String[] itsStringTable;
    final double[] itsDoubleTable;
    final BigInteger[] itsBigIntTable;
    final Object[] itsRegExpLiterals;
    final Object[] itsTemplateLiterals;

    final byte[] itsICode;

    final int maxCalleeArgs;

    final Object[] literalIds;

    final Map<Integer, Integer> longJumps;

    /** PC of the operand of the first LINE icode, or -1 if the code has none. */
    final int firstLineOperandPC;

    /**
     * Source positions, one packed int each: the line in the high half, the column in the low half.
     * Indexed by the operand of a LINE or POS icode.
     *
     * <p>Both halves are unsigned 16-bit. A file with more lines than that already exceeds what a
     * line number can express elsewhere, and a column past 65535 is reported as unknown rather than
     * wrapped.
     */
    final int[] sourcePositions;

    /**
     * Index into {@link #positionSourceNames} per position, or an empty array when no source mapper
     * supplied any. Kept apart from {@link #sourcePositions} because it is unused unless a script
     * was compiled with a source map, which is the less common case.
     */
    final short[] positionSourceIndexes;

    /** Original source paths, or null when no source mapper supplied any. */
    final String[] positionSourceNames;

    /** Shared empty column, since most scripts are compiled without a source map. */
    static final short[] NO_SOURCE_INDEXES = new short[0];

    static final int LINE_SHIFT = 16;
    static final int POSITION_MASK = 0xFFFF;

    /** Packs a line and column into one int, or -1 if either is too large to represent. */
    static int packPosition(int line, int column) {
        if (line < 0 || line > POSITION_MASK || column < 0 || column > POSITION_MASK) return -1;
        return (line << LINE_SHIFT) | column;
    }

    private int icodeHashCode = 0;

    @Override
    public int getLineNumberFromPc(int pc, int pcSourceLineStart) {
        int i = positionIndex(pcSourceLineStart);
        return i < 0 ? 0 : (sourcePositions[i] >>> LINE_SHIFT) & POSITION_MASK;
    }

    @Override
    public int getColumnNumberFromPc(int pc, int pcSourceLineStart) {
        int i = positionIndex(pcSourceLineStart);
        return i < 0 ? 0 : sourcePositions[i] & POSITION_MASK;
    }

    @Override
    public String getSourceNameFromPc(int pc, int pcSourceLineStart) {
        if (positionSourceNames == null) return null;
        int i = positionIndex(pcSourceLineStart);
        if (i < 0 || i >= positionSourceIndexes.length) return null;
        int n = positionSourceIndexes[i];
        return n < 0 ? null : positionSourceNames[n];
    }

    /**
     * Resolves a LINE operand PC to an index into {@link #sourcePositions}. The bounds check also
     * covers data deserialized from a version that had no position table.
     */
    private int positionIndex(int pcSourceLineStart) {
        if (pcSourceLineStart <= 0 || sourcePositions == null) return -1;
        // The operand is one byte or two depending on which form the opcode just before it is.
        int opcode = itsICode[pcSourceLineStart - 1];
        int index;
        if (opcode == Icode.LINE1 || opcode == Icode.POS1) {
            index = itsICode[pcSourceLineStart] & 0xFF;
        } else {
            index =
                    ((itsICode[pcSourceLineStart] & 0xFF) << 8)
                            | (itsICode[pcSourceLineStart + 1] & 0xFF);
        }
        return index < sourcePositions.length ? index : -1;
    }

    public int icodeHashCode() {
        int h = icodeHashCode;
        if (h == 0) {
            icodeHashCode = h = Arrays.hashCode(itsICode);
        }
        return h;
    }

    @Override
    public String toString() {
        return "An idata thing."; // itsSourceFile + ':' + itsName;
    }

    @Override
    public Object execute(
            Context cx,
            T executableObject,
            Object newTarget,
            VarScope scope,
            Object thisObj,
            Object[] args) {
        return Interpreter.interpret(executableObject, this, cx, scope, thisObj, args, newTarget);
    }

    @Override
    public Object resume(
            Context cx,
            T executableObject,
            Object state,
            VarScope scope,
            int operation,
            Object value) {
        return Interpreter.resumeGenerator(cx, scope, operation, state, value);
    }

    public static class Builder<T extends ScriptOrFn<T>> extends JSCode.Builder<T> {
        String[] itsStringTable;
        double[] itsDoubleTable;
        BigInteger[] itsBigIntTable;
        Object[] itsRegExpLiterals;
        Object[] itsTemplateLiterals;

        byte[] itsICode;

        int[] exceptionTable;

        int maxVars;
        int maxLocals;
        int maxStack;
        int maxFrameArray;

        int maxCalleeArgs;

        Object[] literalIds;

        Map<Integer, Integer> longJumps;

        InterpreterData<T> built = null;

        int firstLineOperandPC = -1; // PC of the operand of the first LINE icode

        /** Interned source positions, one packed int each; see InterpreterData#sourcePositions. */
        private int[] sourcePositions = new int[INITIAL_POSITION_TABLE_SIZE];

        /**
         * Source name per position, grown only once a source mapper supplies one. Most scripts are
         * compiled without a source map and never allocate it.
         */
        private short[] positionSourceIndexes = NO_SOURCE_INDEXES;

        private int positionCount;
        private final Map<Position, Integer> positionIndexes = new HashMap<>();
        private final List<String> sourceNames = new ArrayList<>();
        private final Map<String, Integer> sourceNameIndexes = new HashMap<>();

        /**
         * Returns the index of a source position, adding it to the table if it is new. Identical
         * positions share an entry, so loops and re-entered branches cost nothing.
         *
         * <p>The index is emitted as a LINE operand, so it must fit in 16 bits. Scripts with more
         * distinct positions than that reuse the last entry rather than corrupt the table.
         */
        int internSourcePosition(int line, int column, String sourceName) {
            Position key = new Position(sourceName, line, column);
            Integer existing = positionIndexes.get(key);
            if (existing != null) {
                return existing.intValue();
            }
            if (positionCount > 0xFFFF) {
                return positionCount - 1;
            }
            int packed = packPosition(line, column);
            if (packed < 0) {
                // Beyond what a packed position can hold; report the line alone.
                packed = packPosition(Math.min(Math.max(line, 0), POSITION_MASK), 0);
            }
            if (positionCount == sourcePositions.length) {
                sourcePositions = Arrays.copyOf(sourcePositions, sourcePositions.length * 2);
            }
            sourcePositions[positionCount] = packed;
            if (sourceName != null) {
                int nameIndex =
                        sourceNameIndexes
                                .computeIfAbsent(
                                        sourceName,
                                        n -> {
                                            sourceNames.add(n);
                                            return sourceNames.size() - 1;
                                        })
                                .intValue();
                growSourceIndexes(positionCount + 1);
                positionSourceIndexes[positionCount] = (short) nameIndex;
            }
            int index = positionCount++;
            positionIndexes.put(key, index);
            return index;
        }

        /** Grows the source-name column to hold {@code needed} entries, unset ones being -1. */
        private void growSourceIndexes(int needed) {
            if (needed <= positionSourceIndexes.length) return;
            int size = Math.max(needed, Math.max(INITIAL_POSITION_TABLE_SIZE, needed * 2));
            short[] next = new short[size];
            Arrays.fill(next, (short) -1);
            System.arraycopy(positionSourceIndexes, 0, next, 0, positionSourceIndexes.length);
            positionSourceIndexes = next;
        }

        /** The line recorded for a position index; used by the icode dumper. */
        int positionLine(int index) {
            return (sourcePositions[index] >>> LINE_SHIFT) & POSITION_MASK;
        }

        /** The column recorded for a position index; used by the icode dumper. */
        int positionColumn(int index) {
            return sourcePositions[index] & POSITION_MASK;
        }

        public Builder() {
            itsICode = new byte[INITIAL_MAX_ICODE_LENGTH];
            itsStringTable = new String[INITIAL_STRINGTABLE_SIZE];
            itsBigIntTable = new BigInteger[INITIAL_BIGINTTABLE_SIZE];
        }

        @Override
        public JSCode<T> build() {
            if (built == null) {
                var jumpMap = longJumps != null ? Map.copyOf(longJumps) : null;
                built =
                        new InterpreterData<T>(
                                itsStringTable,
                                itsDoubleTable,
                                itsBigIntTable,
                                itsRegExpLiterals,
                                itsTemplateLiterals,
                                itsICode,
                                exceptionTable,
                                maxVars,
                                maxLocals,
                                maxStack,
                                maxFrameArray,
                                maxCalleeArgs,
                                literalIds,
                                jumpMap,
                                firstLineOperandPC,
                                Arrays.copyOf(sourcePositions, positionCount),
                                sourceNames.isEmpty()
                                        ? NO_SOURCE_INDEXES
                                        : Arrays.copyOf(positionSourceIndexes, positionCount),
                                sourceNames.isEmpty() ? null : sourceNames.toArray(new String[0]));
            }
            return built;
        }
    }
}

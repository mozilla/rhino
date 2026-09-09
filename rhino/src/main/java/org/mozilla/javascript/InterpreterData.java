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
            String[] positionSourceNames) {
        super(maxVars, maxLocals, maxStack, maxFrameArray, exceptionTable);
        this.sourcePositions = sourcePositions;
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
     * Source positions, three ints each: line, column, and an index into {@link
     * #positionSourceNames} (negative when the position has no source of its own). Indexed by the
     * operand of a LINE icode.
     */
    final int[] sourcePositions;

    /** Original source paths, or null when no source mapper supplied any. */
    final String[] positionSourceNames;

    private int icodeHashCode = 0;

    @Override
    public int getLineNumberFromPc(int pc, int pcSourceLineStart) {
        int i = positionIndex(pcSourceLineStart);
        return i < 0 ? 0 : sourcePositions[i * 3];
    }

    @Override
    public int getColumnNumberFromPc(int pc, int pcSourceLineStart) {
        int i = positionIndex(pcSourceLineStart);
        return i < 0 ? 0 : sourcePositions[i * 3 + 1];
    }

    @Override
    public String getSourceNameFromPc(int pc, int pcSourceLineStart) {
        if (positionSourceNames == null) return null;
        int i = positionIndex(pcSourceLineStart);
        if (i < 0) return null;
        int n = sourcePositions[i * 3 + 2];
        return n < 0 ? null : positionSourceNames[n];
    }

    /**
     * Resolves a LINE operand PC to an index into {@link #sourcePositions}. The bounds check also
     * covers data deserialized from a version that had no position table.
     */
    private int positionIndex(int pcSourceLineStart) {
        if (pcSourceLineStart < 0 || sourcePositions == null) return -1;
        int index =
                ((itsICode[pcSourceLineStart] & 0xFF) << 8)
                        | (itsICode[pcSourceLineStart + 1] & 0xFF);
        return (index * 3 + 2) < sourcePositions.length ? index : -1;
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

        /** Interned source positions, three ints each; see InterpreterData#sourcePositions. */
        private int[] sourcePositions = new int[INITIAL_POSITION_TABLE_SIZE * 3];

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
            int nameIndex = -1;
            if (sourceName != null) {
                nameIndex =
                        sourceNameIndexes
                                .computeIfAbsent(
                                        sourceName,
                                        n -> {
                                            sourceNames.add(n);
                                            return sourceNames.size() - 1;
                                        })
                                .intValue();
            }
            int base = positionCount * 3;
            if (base + 3 > sourcePositions.length) {
                sourcePositions = Arrays.copyOf(sourcePositions, sourcePositions.length * 2);
            }
            sourcePositions[base] = line;
            sourcePositions[base + 1] = column;
            sourcePositions[base + 2] = nameIndex;
            int index = positionCount++;
            positionIndexes.put(key, index);
            return index;
        }

        /** The line recorded for a position index; used by the icode dumper. */
        int positionLine(int index) {
            return sourcePositions[index * 3];
        }

        /** The column recorded for a position index; used by the icode dumper. */
        int positionColumn(int index) {
            return sourcePositions[index * 3 + 1];
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
                                Arrays.copyOf(sourcePositions, positionCount * 3),
                                sourceNames.isEmpty() ? null : sourceNames.toArray(new String[0]));
            }
            return built;
        }
    }
}

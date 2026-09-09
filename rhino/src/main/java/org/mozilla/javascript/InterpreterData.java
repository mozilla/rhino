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
            long[] positions,
            short[] positionSourceIndexes,
            String[] positionSourceNames) {
        super(maxVars, maxLocals, maxStack, maxFrameArray, exceptionTable);
        this.positions = positions;
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

    // Where the source position changes, ascending by pc. Each entry is the pc in the high half,
    // line and column in the low half. These are kept out of the icode so that the interpreter
    // never spends an instruction on them: only error reporting reads them.
    final long[] positions;

    // Source name per entry, empty unless a source mapper supplied one
    final short[] positionSourceIndexes;
    final String[] positionSourceNames;

    static final short[] NO_SOURCE_INDEXES = new short[0];

    private static final int POSITION_MASK = 0xFFFF;
    private static final int LINE_SHIFT = 16;

    // A line or column past 16 bits is reported as unknown rather than wrapped
    static long packEntry(int pc, int line, int column) {
        int packedLine = line >= 0 && line <= POSITION_MASK ? line : 0;
        int packedColumn = column >= 0 && column <= POSITION_MASK ? column : 0;
        return ((long) pc << 32) | ((long) packedLine << LINE_SHIFT) | packedColumn;
    }

    static int entryPc(long entry) {
        return (int) (entry >>> 32);
    }

    static int entryLine(long entry) {
        return ((int) entry >>> LINE_SHIFT) & POSITION_MASK;
    }

    static int entryColumn(long entry) {
        return (int) entry & POSITION_MASK;
    }

    private int icodeHashCode = 0;

    @Override
    public int getLineNumberFromPc(int pc) {
        int i = positionIndex(pc);
        return i < 0 ? 0 : entryLine(positions[i]);
    }

    @Override
    public int getColumnNumberFromPc(int pc) {
        int i = positionIndex(pc);
        return i < 0 ? 0 : entryColumn(positions[i]);
    }

    @Override
    public String getSourceNameFromPc(int pc) {
        if (positionSourceNames == null) return null;
        int i = positionIndex(pc);
        if (i < 0 || i >= positionSourceIndexes.length) return null;
        int n = positionSourceIndexes[i];
        return n < 0 ? null : positionSourceNames[n];
    }

    long positionAt(int index) {
        return positions[index];
    }

    // The last entry recorded before pc, or -1. Strictly before, since a frame's pc has already
    // moved past the opcode it is executing, while a position is recorded at that opcode's offset.
    int positionIndex(int pc) {
        int lo = 0;
        int hi = positions.length - 1;
        int best = -1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (entryPc(positions[mid]) < pc) {
                best = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return best;
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

        private long[] positions = new long[INITIAL_POSITION_TABLE_SIZE];

        private short[] positionSourceIndexes = NO_SOURCE_INDEXES;
        private int positionCount;
        private List<String> sourceNames;
        private Map<String, Integer> sourceNameIndexes;

        // Successive records at the same pc supersede one another, which happens when a statement
        // updates the position without emitting any code of its own.
        void recordPosition(int pc, int line, int column, String sourceName) {
            long entry = packEntry(pc, line, column);
            boolean sameOffset = positionCount > 0 && entryPc(positions[positionCount - 1]) == pc;
            int at = sameOffset ? positionCount - 1 : positionCount;
            if (!sameOffset && positionCount == positions.length) {
                positions = Arrays.copyOf(positions, positionCount * 2);
            }
            positions[at] = entry;
            if (sourceName != null) {
                if (sourceNames == null) {
                    sourceNames = new ArrayList<>();
                    sourceNameIndexes = new HashMap<>();
                }
                int nameIndex =
                        sourceNameIndexes
                                .computeIfAbsent(
                                        sourceName,
                                        n -> {
                                            sourceNames.add(n);
                                            return sourceNames.size() - 1;
                                        })
                                .intValue();
                growSourceIndexes(at + 1);
                positionSourceIndexes[at] = (short) nameIndex;
            }
            if (!sameOffset) positionCount++;
        }

        // Used by the icode dumper
        long positionEntryFor(int pc) {
            for (int i = positionCount - 1; i >= 0; i--) {
                if (entryPc(positions[i]) == pc) return positions[i];
            }
            return 0;
        }

        private void growSourceIndexes(int needed) {
            if (needed <= positionSourceIndexes.length) return;
            int was = positionSourceIndexes.length;
            int size = Math.max(INITIAL_POSITION_TABLE_SIZE, needed * 2);
            positionSourceIndexes = Arrays.copyOf(positionSourceIndexes, size);
            Arrays.fill(positionSourceIndexes, was, size, (short) -1);
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
                                Arrays.copyOf(positions, positionCount),
                                sourceNames == null
                                        ? NO_SOURCE_INDEXES
                                        : Arrays.copyOf(positionSourceIndexes, positionCount),
                                sourceNames == null ? null : sourceNames.toArray(new String[0]));
            }
            return built;
        }
    }
}

/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Map;

final class InterpreterData<T extends ScriptOrFn<T>> extends ACompilerData<T, InterpreterData<?>>
        implements Serializable {
    @Serial private static final long serialVersionUID = 5067677351589230234L;

    static final int INITIAL_MAX_ICODE_LENGTH = 1024;
    static final int INITIAL_STRINGTABLE_SIZE = 64;
    static final int INITIAL_NUMBERTABLE_SIZE = 64;
    static final int INITIAL_BIGINTTABLE_SIZE = 64;

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
            int firstLinePC) {
        super(maxVars, maxLocals, maxStack, maxFrameArray, exceptionTable);
        this.itsStringTable = itsStringTable;
        this.itsDoubleTable = itsDoubleTable;
        this.itsBigIntTable = itsBigIntTable;
        this.itsRegExpLiterals = itsRegExpLiterals;
        this.itsTemplateLiterals = itsTemplateLiterals;
        this.itsICode = itsICode;
        this.maxCalleeArgs = maxCalleeArgs;
        this.literalIds = literalIds;
        this.longJumps = longJumps;
        this.firstLinePC = firstLinePC;
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

    final int firstLinePC;

    private int icodeHashCode = 0;

    @Override
    public int getLineNumberFromPc(int pc, int pcSourceLineStart) {
        if (pcSourceLineStart >= 0) {
            return ((itsICode[pcSourceLineStart] & 0xFF) << 8)
                    | (itsICode[pcSourceLineStart + 1] & 0xFF);
        } else {
            return 0;
        }
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
        return Interpreter.interpret(executableObject, this, cx, scope, (Scriptable) thisObj, args);
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

        int firstLinePC = -1; // PC for the first LINE icode

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
                                firstLinePC);
            }
            return built;
        }
    }
}

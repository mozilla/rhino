/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Map;
import org.mozilla.javascript.debug.DebuggableScript;

final class InterpreterData implements Serializable, DebuggableScript {
    private static final long serialVersionUID = 5067677351589230234L;

    static final int INITIAL_MAX_ICODE_LENGTH = 1024;
    static final int INITIAL_STRINGTABLE_SIZE = 64;
    static final int INITIAL_NUMBERTABLE_SIZE = 64;
    static final int INITIAL_BIGINTTABLE_SIZE = 64;

    InterpreterData(int languageVersion, String sourceFile, String rawSource, boolean isStrict) {
        this.languageVersion = languageVersion;
        this.itsSourceFile = sourceFile;
        this.rawSource = rawSource;
        this.isStrict = isStrict;
        init();
    }

    InterpreterData(InterpreterData parent) {
        this.parentData = parent;
        this.languageVersion = parent.languageVersion;
        this.itsSourceFile = parent.itsSourceFile;
        this.rawSource = parent.rawSource;
        this.isStrict = parent.isStrict;
        init();
    }

    private void init() {
        itsICode = new byte[INITIAL_MAX_ICODE_LENGTH];
        itsStringTable = new String[INITIAL_STRINGTABLE_SIZE];
        itsBigIntTable = new BigInteger[INITIAL_BIGINTTABLE_SIZE];
    }

    String itsName;
    String itsSourceFile;
    boolean itsNeedsActivation;
    int itsFunctionType;

    String[] itsStringTable;
    double[] itsDoubleTable;
    BigInteger[] itsBigIntTable;
    InterpreterData[] itsNestedFunctions;
    Object[] itsRegExpLiterals;
    Object[] itsTemplateLiterals;

    byte[] itsICode;

    int[] itsExceptionTable;

    int itsMaxVars;
    int itsMaxLocals;
    int itsMaxStack;
    int itsMaxFrameArray;

    // see comments in NativeFuncion for definition of argNames and argCount
    String[] argNames;
    boolean[] argIsConst;
    int argCount;
    boolean argsHasRest;
    boolean argsHasDefaults;

    int itsMaxCalleeArgs;

    String rawSource;
    int rawSourceStart;
    int rawSourceEnd;

    int languageVersion;

    boolean isStrict;
    boolean topLevel;
    boolean isES6Generator;

    Object[] literalIds;

    Map<Integer, Integer> longJumps;

    int firstLinePC = -1; // PC for the first LINE icode

    InterpreterData parentData;

    boolean evalScriptFlag; // true if script corresponds to eval() code

    private int icodeHashCode = 0;

    /** true if the function has been declared like "!function() {}". */
    boolean declaredAsFunctionExpression;

    @Override
    public boolean isTopLevel() {
        return topLevel;
    }

    @Override
    public boolean isFunction() {
        return itsFunctionType != 0;
    }

    @Override
    public String getFunctionName() {
        return itsName;
    }

    @Override
    public int getParamCount() {
        return argCount;
    }

    @Override
    public int getParamAndVarCount() {
        return argNames.length;
    }

    @Override
    public String getParamOrVarName(int index) {
        return argNames[index];
    }

    public boolean getParamOrVarConst(int index) {
        return argIsConst[index];
    }

    @Override
    public String getSourceName() {
        return itsSourceFile;
    }

    @Override
    public boolean isGeneratedScript() {
        return ScriptRuntime.isGeneratedScript(itsSourceFile);
    }

    @Override
    public int[] getLineNumbers() {
        return Interpreter.getLineNumbers(this);
    }

    @Override
    public int getFunctionCount() {
        return (itsNestedFunctions == null) ? 0 : itsNestedFunctions.length;
    }

    @Override
    public DebuggableScript getFunction(int index) {
        return itsNestedFunctions[index];
    }

    @Override
    public DebuggableScript getParent() {
        return parentData;
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
        return itsSourceFile + ':' + itsName;
    }
}

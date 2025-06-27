/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import static org.mozilla.javascript.UniqueTag.DOUBLE_MARK;

import java.io.PrintStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import org.mozilla.javascript.ScriptRuntime.NoSuchMethodShim;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.ScriptNode;
import org.mozilla.javascript.debug.DebugFrame;

public final class Interpreter extends Icode implements Evaluator {
    // data for parsing
    InterpreterData itsData;

    static final int EXCEPTION_TRY_START_SLOT = 0;
    static final int EXCEPTION_TRY_END_SLOT = 1;
    static final int EXCEPTION_HANDLER_SLOT = 2;
    static final int EXCEPTION_TYPE_SLOT = 3;
    static final int EXCEPTION_LOCAL_SLOT = 4;
    static final int EXCEPTION_SCOPE_SLOT = 5;
    // SLOT_SIZE: space for try start/end, handler, start, handler type,
    //            exception local and scope local
    static final int EXCEPTION_SLOT_SIZE = 6;

    /** Class to hold data corresponding to one interpreted call stack frame. */
    private static class CallFrame implements Cloneable, Serializable {
        private static final long serialVersionUID = -2843792508994958978L;

        // fields marked "final" in a comment are effectively final except when they're modified
        // immediately after cloning.

        final CallFrame parentFrame;
        // amount of stack frames before this one on the interpretation stack
        final short frameIndex;
        // The frame that the iterator was executing.
        final CallFrame previousInterpreterFrame;
        final int parentPC;
        // If true indicates read-only frame that is a part of continuation
        boolean frozen;

        final InterpretedFunction fnOrScript;
        final InterpreterData idata;

        // Stack structure
        // stack[0 <= i < localShift]: arguments and local variables
        // stack[localShift <= i <= emptyStackTop]: used for local temporaries
        // stack[emptyStackTop < i < stack.length]: stack data
        // sDbl[i]: if stack[i] is UniqueTag.DOUBLE_MARK, sDbl[i] holds the number value

        final Object[] stack;
        final byte[] stackAttributes;
        final double[] sDbl;

        final CallFrame varSource; // defaults to this unless continuation frame
        final short emptyStackTop;

        final DebugFrame debuggerFrame;
        final boolean useActivation;
        boolean isContinuationsTopFrame;

        final Scriptable thisObj;

        // The values that change during interpretation

        Object result;
        double resultDbl;
        int pc;
        int pcPrevBranch;
        int pcSourceLineStart;
        Scriptable scope;

        int savedStackTop;
        int savedCallOp;
        Object throwable;

        CallFrame(
                Context cx,
                Scriptable thisObj,
                InterpretedFunction fnOrScript,
                CallFrame parentFrame,
                CallFrame previousInterpreterFrame) {
            idata = fnOrScript.idata;
            debuggerFrame = cx.debugger != null ? cx.debugger.getFrame(cx, idata) : null;
            useActivation = debuggerFrame != null || idata.itsNeedsActivation;

            emptyStackTop = (short) (idata.itsMaxVars + idata.itsMaxLocals - 1);
            int maxFrameArray = idata.itsMaxFrameArray;
            if (maxFrameArray != emptyStackTop + idata.itsMaxStack + 1) Kit.codeBug();

            stack = new Object[maxFrameArray];
            stackAttributes = new byte[maxFrameArray];
            sDbl = new double[maxFrameArray];

            this.fnOrScript = fnOrScript;
            varSource = this;
            this.thisObj = thisObj;

            this.parentFrame = parentFrame;
            if (parentFrame == null) {
                this.parentPC =
                        previousInterpreterFrame == null
                                ? -1
                                : previousInterpreterFrame.pcSourceLineStart;
            } else {
                this.parentPC = parentFrame.pcSourceLineStart;
            }
            this.previousInterpreterFrame = previousInterpreterFrame;
            frameIndex = (short) ((parentFrame == null) ? 0 : parentFrame.frameIndex + 1);
            if (frameIndex > cx.getMaximumInterpreterStackDepth()) {
                throw Context.reportRuntimeError("Exceeded maximum stack depth");
            }

            // Initialize initial values of variables that change during
            // interpretation.
            result = Undefined.instance;
            pcSourceLineStart = idata.firstLinePC;

            savedStackTop = emptyStackTop;
        }

        private CallFrame(CallFrame original, boolean makeOrphan) {
            this(
                    original,
                    makeOrphan ? null : original.parentFrame,
                    makeOrphan ? null : original.previousInterpreterFrame);
        }

        /* Copy the frame for *continuations*. Here we want to make
        fresh copies of the stack and everything related to it. */
        private CallFrame(
                CallFrame original, CallFrame parentFrame, CallFrame previousInterpreterFrame) {
            if (!original.frozen) Kit.codeBug();

            stack = Arrays.copyOf(original.stack, original.stack.length);
            stackAttributes =
                    Arrays.copyOf(original.stackAttributes, original.stackAttributes.length);
            sDbl = Arrays.copyOf(original.sDbl, original.sDbl.length);

            frozen = false;
            this.parentFrame = parentFrame;
            this.previousInterpreterFrame = previousInterpreterFrame;
            if (parentFrame == null) {
                frameIndex = 0;
                parentPC =
                        previousInterpreterFrame == null
                                ? -1
                                : previousInterpreterFrame.pcSourceLineStart;
            } else {
                frameIndex = original.frameIndex;
                parentPC = parentFrame.pcSourceLineStart;
            }

            fnOrScript = original.fnOrScript;
            idata = original.idata;

            varSource = original.varSource;
            emptyStackTop = original.emptyStackTop;

            debuggerFrame = original.debuggerFrame;
            useActivation = original.useActivation;
            isContinuationsTopFrame = original.isContinuationsTopFrame;

            thisObj = original.thisObj;

            result = original.result;
            resultDbl = original.resultDbl;
            pc = original.pc;
            pcPrevBranch = original.pcPrevBranch;
            pcSourceLineStart = original.pcSourceLineStart;
            scope = original.scope;

            savedStackTop = original.savedStackTop;
            savedCallOp = original.savedCallOp;
            throwable = original.throwable;
        }

        /* Copy the stack for running a generator. We're only doing
        this to maintain the correct chain of parents for exception
        stacks, so we'll reuse the existing stack arrays. */
        private CallFrame(
                CallFrame original,
                CallFrame parentFrame,
                CallFrame previousInterpreterFrame,
                boolean keepFrozen) {
            if (!original.frozen) Kit.codeBug();

            stack = original.stack;
            stackAttributes = original.stackAttributes;
            sDbl = original.sDbl;

            frozen = keepFrozen;
            this.parentFrame = parentFrame;
            this.previousInterpreterFrame = previousInterpreterFrame;
            if (parentFrame == null) {
                frameIndex = 0;
                parentPC =
                        previousInterpreterFrame == null
                                ? -1
                                : previousInterpreterFrame.pcSourceLineStart;
            } else {
                frameIndex = original.frameIndex;
                parentPC = parentFrame.pcSourceLineStart;
            }

            fnOrScript = original.fnOrScript;
            idata = original.idata;

            varSource = original.varSource;
            emptyStackTop = original.emptyStackTop;

            debuggerFrame = original.debuggerFrame;
            useActivation = original.useActivation;
            isContinuationsTopFrame = original.isContinuationsTopFrame;

            thisObj = original.thisObj;

            result = original.result;
            resultDbl = original.resultDbl;
            pc = original.pc;
            pcPrevBranch = original.pcPrevBranch;
            pcSourceLineStart = original.pcSourceLineStart;
            scope = original.scope;

            savedStackTop = original.savedStackTop;
            savedCallOp = original.savedCallOp;
            throwable = original.throwable;
        }

        void initializeArgs(
                Context cx,
                Scriptable callerScope,
                Object[] args,
                double[] argsDbl,
                Object[] boundArgs,
                int argShift,
                int argCount,
                Scriptable homeObject) {
            if (useActivation) {
                // Copy args to new array to pass to enterActivationFunction
                // or debuggerFrame.onEnter
                if (argsDbl != null || boundArgs != null) {
                    int blen = boundArgs == null ? 0 : boundArgs.length;
                    args = getArgsArray(args, argsDbl, boundArgs, blen, argShift, argCount);
                }
                argShift = 0;
                argsDbl = null;
                boundArgs = null;
            }

            if (idata.itsFunctionType != 0) {
                scope = fnOrScript.getParentScope();

                if (useActivation) {
                    if (idata.itsFunctionType == FunctionNode.ARROW_FUNCTION) {
                        scope =
                                ScriptRuntime.createArrowFunctionActivation(
                                        fnOrScript,
                                        cx,
                                        scope,
                                        args,
                                        fnOrScript.isStrict(),
                                        idata.argsHasRest,
                                        idata.itsRequiresArgumentObject,
                                        homeObject);
                    } else {
                        scope =
                                ScriptRuntime.createFunctionActivation(
                                        fnOrScript,
                                        cx,
                                        scope,
                                        args,
                                        fnOrScript.isStrict(),
                                        idata.argsHasRest,
                                        idata.itsRequiresArgumentObject,
                                        homeObject);
                    }
                }
            } else {
                scope = callerScope;
                ScriptRuntime.initScript(fnOrScript, thisObj, cx, scope, idata.evalScriptFlag);
            }

            if (idata.itsNestedFunctions != null) {
                if (idata.itsFunctionType != 0 && !idata.itsNeedsActivation) Kit.codeBug();
                for (int i = 0; i < idata.itsNestedFunctions.length; i++) {
                    InterpreterData fdata = idata.itsNestedFunctions[i];
                    if (fdata.itsFunctionType == FunctionNode.FUNCTION_STATEMENT) {
                        initFunction(cx, scope, fnOrScript, i);
                    }
                }
            }

            int varCount = idata.getParamAndVarCount();
            for (int i = 0; i < varCount; i++) {
                if (idata.getParamOrVarConst(i)) stackAttributes[i] = ScriptableObject.CONST;
            }
            int definedArgs = idata.argCount;
            if (definedArgs > argCount) {
                definedArgs = argCount;
            }

            // Fill the frame structure

            int blen = 0;
            if (boundArgs != null) {
                blen = Math.min(definedArgs, boundArgs.length);
                System.arraycopy(boundArgs, 0, stack, 0, blen);
            }

            System.arraycopy(args, argShift, stack, blen, definedArgs - blen);
            if (argsDbl != null) {
                System.arraycopy(argsDbl, argShift, sDbl, blen, definedArgs - blen);
            }
            for (int i = definedArgs; i != idata.itsMaxVars; ++i) {
                stack[i] = Undefined.instance;
            }

            if (idata.argsHasRest) {
                Object[] vals;
                int offset = idata.argCount - 1;
                if (argCount >= idata.argCount) {
                    vals = new Object[argCount - offset];

                    argShift = argShift + offset;
                    for (int valsIdx = 0; valsIdx != vals.length; ++argShift, ++valsIdx) {
                        Object val = args[argShift];
                        if (val == UniqueTag.DOUBLE_MARK) {
                            val = ScriptRuntime.wrapNumber(argsDbl[argShift]);
                        }
                        vals[valsIdx] = val;
                    }
                } else {
                    vals = ScriptRuntime.emptyArgs;
                }
                stack[offset] = cx.newArray(scope, vals);
            }
        }

        CallFrame cloneFrozen() {
            return new CallFrame(this, false);
        }

        CallFrame shallowCloneFrozen(CallFrame newPreviousInterpreeterFrame) {
            return new CallFrame(this, this.parentFrame, newPreviousInterpreeterFrame, true);
        }

        void syncStateToFrame(CallFrame otherFrame) {
            otherFrame.frozen = frozen;
            otherFrame.isContinuationsTopFrame = isContinuationsTopFrame;
            otherFrame.result = result;
            otherFrame.resultDbl = resultDbl;
            otherFrame.pc = pc;
            otherFrame.pcPrevBranch = pcPrevBranch;
            otherFrame.pcSourceLineStart = pcSourceLineStart;
            otherFrame.scope = scope;

            otherFrame.savedStackTop = savedStackTop;
            otherFrame.savedCallOp = savedCallOp;
            otherFrame.throwable = throwable;
        }

        @Override
        public boolean equals(Object other) {
            // Overridden for semantic equality comparison. These objects
            // are typically exposed as NativeContinuation.implementation,
            // comparing them allows establishing whether the continuations
            // are semantically equal.
            if (other instanceof CallFrame) {
                // If the call is not within a Context with a top call, we force
                // one. It is required as some objects within fully initialized
                // global scopes (notably, XMLLibImpl) need to have a top scope
                // in order to evaluate their attributes.
                try (Context cx = Context.enter()) {
                    if (ScriptRuntime.hasTopCall(cx)) {
                        return equalsInTopScope(other).booleanValue();
                    }
                    final Scriptable top = ScriptableObject.getTopLevelScope(scope);
                    return ((Boolean)
                                    ScriptRuntime.doTopCall(
                                            (c, scope, thisObj, args) -> equalsInTopScope(other),
                                            cx,
                                            top,
                                            top,
                                            ScriptRuntime.emptyArgs,
                                            isStrictTopFrame()))
                            .booleanValue();
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            // Overridden for consistency with equals.
            // Trying to strike a balance between speed of calculation and
            // distribution. Not hashing stack variables as those could have
            // unbounded computational cost and limit it to topmost 8 frames.
            int depth = 0;
            CallFrame f = this;
            int h = 0;
            do {
                h = 31 * (31 * h + f.pc) + f.idata.icodeHashCode();
                f = f.parentFrame;
            } while (f != null && depth++ < 8);
            return h;
        }

        private Boolean equalsInTopScope(Object other) {
            return EqualObjectGraphs.withThreadLocal(eq -> equals(this, (CallFrame) other, eq));
        }

        private boolean isStrictTopFrame() {
            CallFrame f = this;
            for (; ; ) {
                final CallFrame p = f.parentFrame;
                if (p == null) {
                    return f.fnOrScript.isStrict();
                }
                f = p;
            }
        }

        @SuppressWarnings("ReferenceEquality")
        private static Boolean equals(CallFrame f1, CallFrame f2, EqualObjectGraphs equal) {
            // Iterative instead of recursive, as interpreter stack depth can
            // be larger than JVM stack depth.
            for (; ; ) {
                if (f1 == f2) {
                    return Boolean.TRUE;
                } else if (f1 == null || f2 == null) {
                    return Boolean.FALSE;
                } else if (!f1.fieldsEqual(f2, equal)) {
                    return Boolean.FALSE;
                } else {
                    f1 = f1.parentFrame;
                    f2 = f2.parentFrame;
                }
            }
        }

        private boolean fieldsEqual(CallFrame other, EqualObjectGraphs equal) {
            return frameIndex == other.frameIndex
                    && pc == other.pc
                    && compareIdata(idata, other.idata)
                    && equal.equalGraphs(varSource.stack, other.varSource.stack)
                    && Arrays.equals(varSource.sDbl, other.varSource.sDbl)
                    && equal.equalGraphs(thisObj, other.thisObj)
                    && equal.equalGraphs(fnOrScript, other.fnOrScript)
                    && equal.equalGraphs(scope, other.scope);
        }

        CallFrame captureForGenerator() {
            return new CallFrame(this, true);
        }
    }

    private static boolean compareIdata(InterpreterData i1, InterpreterData i2) {
        return i1 == i2 || Objects.equals(getRawSource(i1), getRawSource(i2));
    }

    private static final class ContinuationJump implements Serializable {
        private static final long serialVersionUID = 7687739156004308247L;

        CallFrame capturedFrame;
        CallFrame branchFrame;
        Object result;
        double resultDbl;

        ContinuationJump(NativeContinuation c, CallFrame current) {
            this.capturedFrame = (CallFrame) c.getImplementation();
            if (this.capturedFrame == null || current == null) {
                // Continuation and current execution does not share
                // any frames if there is nothing to capture or
                // if there is no currently executed frames
                this.branchFrame = null;
            } else {
                // Search for branch frame where parent frame chains starting
                // from captured and current meet.
                CallFrame chain1 = this.capturedFrame;
                CallFrame chain2 = current;

                // First work parents of chain1 or chain2 until the same
                // frame depth.
                int diff = chain1.frameIndex - chain2.frameIndex;
                if (diff != 0) {
                    if (diff < 0) {
                        // swap to make sure that
                        // chain1.frameIndex > chain2.frameIndex and diff > 0
                        chain1 = current;
                        chain2 = this.capturedFrame;
                        diff = -diff;
                    }
                    do {
                        chain1 = chain1.parentFrame;
                    } while (--diff != 0);
                    if (chain1.frameIndex != chain2.frameIndex) Kit.codeBug();
                }

                // Now walk parents in parallel until a shared frame is found
                // or until the root is reached.
                while (!Objects.equals(chain1, chain2) && chain1 != null) {
                    chain1 = chain1.parentFrame;
                    chain2 = chain2.parentFrame;
                }

                this.branchFrame = chain1;
                if (this.branchFrame != null && !this.branchFrame.frozen) Kit.codeBug();
            }
        }
    }

    private static CallFrame captureFrameForGenerator(CallFrame frame) {
        frame.frozen = true;
        CallFrame result = frame.captureForGenerator();
        frame.frozen = false;

        return result;
    }

    static {
        // Checks for byte code consistencies, good compiler can eliminate them

        if (Token.LAST_BYTECODE_TOKEN > 127) {
            String str = "Violation of Token.LAST_BYTECODE_TOKEN <= 127";
            System.err.println(str);
            throw new IllegalStateException(str);
        }
        if (MIN_ICODE < -128) {
            String str = "Violation of Interpreter.MIN_ICODE >= -128";
            System.err.println(str);
            throw new IllegalStateException(str);
        }
    }

    @Override
    public Object compile(
            CompilerEnvirons compilerEnv,
            ScriptNode tree,
            String rawSource,
            boolean returnFunction) {
        CodeGenerator cgen = new CodeGenerator();
        itsData = cgen.compile(compilerEnv, tree, rawSource, returnFunction);
        return itsData;
    }

    @Override
    public Script createScriptObject(Object bytecode, Object staticSecurityDomain) {
        if (bytecode != itsData) {
            Kit.codeBug();
        }
        return InterpretedFunction.createScript(itsData, staticSecurityDomain);
    }

    @Override
    public void setEvalScriptFlag(Script script) {
        ((InterpretedFunction) script).idata.evalScriptFlag = true;
    }

    @Override
    public Function createFunctionObject(
            Context cx, Scriptable scope, Object bytecode, Object staticSecurityDomain) {
        if (bytecode != itsData) {
            Kit.codeBug();
        }
        return InterpretedFunction.createFunction(cx, scope, itsData, staticSecurityDomain);
    }

    private static int getShort(byte[] iCode, int pc) {
        return (iCode[pc] << 8) | (iCode[pc + 1] & 0xFF);
    }

    private static int getIndex(byte[] iCode, int pc) {
        return ((iCode[pc] & 0xFF) << 8) | (iCode[pc + 1] & 0xFF);
    }

    private static int getInt(byte[] iCode, int pc) {
        return (iCode[pc] << 24)
                | ((iCode[pc + 1] & 0xFF) << 16)
                | ((iCode[pc + 2] & 0xFF) << 8)
                | (iCode[pc + 3] & 0xFF);
    }

    private static int getExceptionHandler(CallFrame frame, boolean onlyFinally) {
        int[] exceptionTable = frame.idata.itsExceptionTable;
        if (exceptionTable == null) {
            // No exception handlers
            return -1;
        }

        // Icode switch in the interpreter increments PC immediately
        // and it is necessary to subtract 1 from the saved PC
        // to point it before the start of the next instruction.
        int pc = frame.pc - 1;

        // OPT: use binary search
        int best = -1, bestStart = 0, bestEnd = 0;
        for (int i = 0; i != exceptionTable.length; i += EXCEPTION_SLOT_SIZE) {
            int start = exceptionTable[i + EXCEPTION_TRY_START_SLOT];
            int end = exceptionTable[i + EXCEPTION_TRY_END_SLOT];
            if (!(start <= pc && pc < end)) {
                continue;
            }
            if (onlyFinally && exceptionTable[i + EXCEPTION_TYPE_SLOT] != 1) {
                continue;
            }
            if (best >= 0) {
                // Since handlers always nest and they never have shared end
                // although they can share start  it is sufficient to compare
                // handlers ends
                if (bestEnd < end) {
                    continue;
                }
                // Check the above assumption
                if (bestStart > start) Kit.codeBug(); // should be nested
                if (bestEnd == end) Kit.codeBug(); // no ens sharing
            }
            best = i;
            bestStart = start;
            bestEnd = end;
        }
        return best;
    }

    static void dumpICode(InterpreterData idata) {
        if (!Token.printICode) {
            return;
        }

        byte[] iCode = idata.itsICode;
        int iCodeLength = iCode.length;
        String[] strings = idata.itsStringTable;
        BigInteger[] bigInts = idata.itsBigIntTable;
        PrintStream out = System.out;
        out.println("ICode dump, for " + idata.itsName + ", length = " + iCodeLength);
        out.println("MaxStack = " + idata.itsMaxStack);

        int indexReg = 0;
        for (int pc = 0; pc < iCodeLength; ) {
            out.flush();
            out.print(" [" + pc + "] ");
            int token = iCode[pc];
            int icodeLength = bytecodeSpan(token);
            String tname = Icode.bytecodeName(token);
            int old_pc = pc;
            ++pc;
            switch (token) {
                default:
                    if (icodeLength != 1) Kit.codeBug();
                    out.println(tname);
                    break;

                case Icode_GOSUB:
                case Token.GOTO:
                case Token.IFEQ:
                case Token.IFNE:
                case Icode_IFEQ_POP:
                case Icode_IF_NULL_UNDEF:
                case Icode_IF_NOT_NULL_UNDEF:
                case Icode_LEAVEDQ:
                    {
                        int newPC = pc + getShort(iCode, pc) - 1;
                        out.println(tname + " " + newPC);
                        pc += 2;
                        break;
                    }
                case Icode_VAR_INC_DEC:
                case Icode_NAME_INC_DEC:
                case Icode_PROP_INC_DEC:
                case Icode_ELEM_INC_DEC:
                case Icode_REF_INC_DEC:
                    {
                        int incrDecrType = iCode[pc];
                        out.println(tname + " " + incrDecrType);
                        ++pc;
                        break;
                    }

                case Icode_CALLSPECIAL:
                case Icode_CALLSPECIAL_OPTIONAL:
                    {
                        int callType = iCode[pc] & 0xFF;
                        boolean isNew = (iCode[pc + 1] != 0);
                        int line = getIndex(iCode, pc + 2);
                        out.println(
                                tname + " " + callType + " " + isNew + " " + indexReg + " " + line);
                        pc += 4;
                        break;
                    }

                case Token.CATCH_SCOPE:
                    {
                        boolean afterFisrtFlag = (iCode[pc] != 0);
                        out.println(tname + " " + afterFisrtFlag);
                        ++pc;
                    }
                    break;
                case Token.REGEXP:
                    out.println(tname + " " + idata.itsRegExpLiterals[indexReg]);
                    break;
                case Icode_LITERAL_NEW_OBJECT:
                    {
                        boolean copyArray = iCode[pc++] != 0;
                        Object[] keys = (Object[]) idata.literalIds[indexReg];
                        out.println(tname + " " + Arrays.toString(keys) + " " + copyArray);
                        break;
                    }
                case Icode_SPARE_ARRAYLIT:
                    out.println(tname + " " + idata.literalIds[indexReg]);
                    break;
                case Icode_CLOSURE_EXPR:
                case Icode_CLOSURE_STMT:
                    out.println(tname + " " + idata.itsNestedFunctions[indexReg]);
                    break;
                case Token.CALL:
                case Icode_CALL_ON_SUPER:
                case Icode_TAIL_CALL:
                case Token.REF_CALL:
                case Token.NEW:
                    out.println(tname + ' ' + indexReg);
                    break;
                case Token.THROW:
                case Token.YIELD:
                case Icode_YIELD_STAR:
                case Icode_GENERATOR:
                case Icode_GENERATOR_END:
                case Icode_GENERATOR_RETURN:
                    {
                        int line = getIndex(iCode, pc);
                        out.println(tname + " : " + line);
                        pc += 2;
                        break;
                    }
                case Icode_SHORTNUMBER:
                    {
                        int value = getShort(iCode, pc);
                        out.println(tname + " " + value);
                        pc += 2;
                        break;
                    }
                case Icode_INTNUMBER:
                    {
                        int value = getInt(iCode, pc);
                        out.println(tname + " " + value);
                        pc += 4;
                        break;
                    }
                case Token.NUMBER:
                    {
                        double value = idata.itsDoubleTable[indexReg];
                        out.println(tname + " " + value);
                        break;
                    }
                case Icode_LINE:
                    {
                        int line = getIndex(iCode, pc);
                        out.println(tname + " : " + line);
                        pc += 2;
                        break;
                    }
                case Icode_REG_STR1:
                    {
                        String str = strings[0xFF & iCode[pc]];
                        out.println(tname + " \"" + str + '"');
                        ++pc;
                        break;
                    }
                case Icode_REG_STR2:
                    {
                        String str = strings[getIndex(iCode, pc)];
                        out.println(tname + " \"" + str + '"');
                        pc += 2;
                        break;
                    }
                case Icode_REG_STR4:
                    {
                        String str = strings[getInt(iCode, pc)];
                        out.println(tname + " \"" + str + '"');
                        pc += 4;
                        break;
                    }
                case Icode_REG_STR_C0:
                    {
                        String str = strings[0];
                        out.println(tname + " \"" + str + '"');
                        break;
                    }
                case Icode_REG_STR_C1:
                    {
                        String str = strings[1];
                        out.println(tname + " \"" + str + '"');
                        break;
                    }
                case Icode_REG_STR_C2:
                    {
                        String str = strings[2];
                        out.println(tname + " \"" + str + '"');
                        break;
                    }
                case Icode_REG_STR_C3:
                    {
                        String str = strings[3];
                        out.println(tname + " \"" + str + '"');
                        break;
                    }
                case Icode_REG_IND_C0:
                    indexReg = 0;
                    out.println(tname);
                    break;
                case Icode_REG_IND_C1:
                    indexReg = 1;
                    out.println(tname);
                    break;
                case Icode_REG_IND_C2:
                    indexReg = 2;
                    out.println(tname);
                    break;
                case Icode_REG_IND_C3:
                    indexReg = 3;
                    out.println(tname);
                    break;
                case Icode_REG_IND_C4:
                    indexReg = 4;
                    out.println(tname);
                    break;
                case Icode_REG_IND_C5:
                    indexReg = 5;
                    out.println(tname);
                    break;
                case Icode_REG_IND1:
                    {
                        indexReg = 0xFF & iCode[pc];
                        out.println(tname + " " + indexReg);
                        ++pc;
                        break;
                    }
                case Icode_REG_IND2:
                    {
                        indexReg = getIndex(iCode, pc);
                        out.println(tname + " " + indexReg);
                        pc += 2;
                        break;
                    }
                case Icode_REG_IND4:
                    {
                        indexReg = getInt(iCode, pc);
                        out.println(tname + " " + indexReg);
                        pc += 4;
                        break;
                    }
                case Icode_GETVAR1:
                case Icode_SETVAR1:
                case Icode_SETCONSTVAR1:
                    indexReg = iCode[pc];
                    out.println(tname + " " + indexReg);
                    ++pc;
                    break;
                // TODO: Icode_REG_STR_C0-3 is not dump. I made this the same it.
                case Icode_REG_BIGINT_C0:
                case Icode_REG_BIGINT_C1:
                case Icode_REG_BIGINT_C2:
                case Icode_REG_BIGINT_C3:
                    Kit.codeBug();
                    break;
                case Icode_REG_BIGINT1:
                    {
                        BigInteger bigInt = bigInts[0xFF & iCode[pc]];
                        out.println(tname + " " + bigInt.toString() + 'n');
                        ++pc;
                        break;
                    }
                case Icode_REG_BIGINT2:
                    {
                        BigInteger bigInt = bigInts[getIndex(iCode, pc)];
                        out.println(tname + " " + bigInt.toString() + 'n');
                        pc += 2;
                        break;
                    }
                case Icode_REG_BIGINT4:
                    {
                        BigInteger bigInt = bigInts[getInt(iCode, pc)];
                        out.println(tname + " " + bigInt.toString() + 'n');
                        pc += 4;
                        break;
                    }
            }
            if (old_pc + icodeLength != pc) Kit.codeBug();
        }

        int[] table = idata.itsExceptionTable;
        if (table != null) {
            out.println("Exception handlers: " + table.length / EXCEPTION_SLOT_SIZE);
            for (int i = 0; i != table.length; i += EXCEPTION_SLOT_SIZE) {
                int tryStart = table[i + EXCEPTION_TRY_START_SLOT];
                int tryEnd = table[i + EXCEPTION_TRY_END_SLOT];
                int handlerStart = table[i + EXCEPTION_HANDLER_SLOT];
                int type = table[i + EXCEPTION_TYPE_SLOT];
                int exceptionLocal = table[i + EXCEPTION_LOCAL_SLOT];

                out.println(
                        " tryStart="
                                + tryStart
                                + " tryEnd="
                                + tryEnd
                                + " handlerStart="
                                + handlerStart
                                + " type="
                                + (type == 0 ? "catch" : "finally")
                                + " exceptionLocal="
                                + exceptionLocal);
            }
        }
        out.flush();
    }

    private static int bytecodeSpan(int bytecode) {
        switch (bytecode) {
            case Token.THROW:
            case Token.YIELD:
            case Icode_YIELD_STAR:
            case Icode_GENERATOR:
            case Icode_GENERATOR_END:
            case Icode_GENERATOR_RETURN:
                // source line
                return 1 + 2;

            case Icode_GOSUB:
            case Token.GOTO:
            case Token.IFEQ:
            case Token.IFNE:
            case Icode_IFEQ_POP:
            case Icode_IF_NULL_UNDEF:
            case Icode_IF_NOT_NULL_UNDEF:
            case Icode_LEAVEDQ:
                // target pc offset
                return 1 + 2;

            case Icode_CALLSPECIAL:
            case Icode_CALLSPECIAL_OPTIONAL:
                // call type
                // is new
                // line number
                return 1 + 1 + 1 + 2;

            case Token.CATCH_SCOPE:
                // scope flag
                return 1 + 1;

            case Icode_VAR_INC_DEC:
            case Icode_NAME_INC_DEC:
            case Icode_PROP_INC_DEC:
            case Icode_ELEM_INC_DEC:
            case Icode_REF_INC_DEC:
                // type of ++/--
                return 1 + 1;

            case Icode_SHORTNUMBER:
                // short number
                return 1 + 2;

            case Icode_INTNUMBER:
                // int number
                return 1 + 4;

            case Icode_REG_IND1:
                // ubyte index
                return 1 + 1;

            case Icode_REG_IND2:
                // ushort index
                return 1 + 2;

            case Icode_REG_IND4:
                // int index
                return 1 + 4;

            case Icode_REG_STR1:
                // ubyte string index
                return 1 + 1;

            case Icode_REG_STR2:
                // ushort string index
                return 1 + 2;

            case Icode_REG_STR4:
                // int string index
                return 1 + 4;

            case Icode_GETVAR1:
            case Icode_SETVAR1:
            case Icode_SETCONSTVAR1:
                // byte var index
                return 1 + 1;

            case Icode_LINE:
                // line number
                return 1 + 2;

            case Icode_LITERAL_NEW_OBJECT:
                // make a copy or not flag
                return 1 + 1;
        }
        if (!validBytecode(bytecode)) throw Kit.codeBug();
        return 1;
    }

    static int[] getLineNumbers(InterpreterData data) {
        HashSet<Integer> presentLines = new HashSet<>();

        byte[] iCode = data.itsICode;
        int iCodeLength = iCode.length;
        for (int pc = 0; pc != iCodeLength; ) {
            int bytecode = iCode[pc];
            int span = bytecodeSpan(bytecode);
            if (bytecode == Icode_LINE) {
                if (span != 3) Kit.codeBug();
                int line = getIndex(iCode, pc + 1);
                presentLines.add(line);
            }
            pc += span;
        }

        int[] ret = new int[presentLines.size()];
        int i = 0;
        for (int num : presentLines) {
            ret[i++] = num;
        }
        return ret;
    }

    @Override
    public void captureStackInfo(RhinoException ex) {
        Context cx = Context.getCurrentContext();
        if (cx == null || cx.lastInterpreterFrame == null) {
            // No interpreter invocations
            ex.interpreterStackInfo = null;
        } else {
            ex.interpreterStackInfo = cx.lastInterpreterFrame;
        }
    }

    @Override
    public String getSourcePositionFromStack(Context cx, int[] linep) {
        CallFrame frame = (CallFrame) cx.lastInterpreterFrame;
        InterpreterData idata = frame.idata;
        if (frame.pcSourceLineStart >= 0) {
            linep[0] = getIndex(idata.itsICode, frame.pcSourceLineStart);
        } else {
            linep[0] = 0;
        }
        return idata.itsSourceFile;
    }

    @Override
    public String getPatchedStack(RhinoException ex, String nativeStackTrace) {
        String tag = "org.mozilla.javascript.Interpreter.interpretLoop";
        StringBuilder sb = new StringBuilder(nativeStackTrace.length() + 1000);
        String lineSeparator = SecurityUtilities.getSystemProperty("line.separator");

        CallFrame calleeFrame = null;
        CallFrame frame = (CallFrame) ex.interpreterStackInfo;
        int offset = 0;
        while (frame != null) {
            CallFrame callerFrame = frame;
            int pos = nativeStackTrace.indexOf(tag, offset);
            if (pos < 0) {
                break;
            }

            // Skip tag length
            pos += tag.length();
            // Skip until the end of line
            for (; pos != nativeStackTrace.length(); ++pos) {
                char c = nativeStackTrace.charAt(pos);
                if (c == '\n' || c == '\r') {
                    break;
                }
            }
            sb.append(nativeStackTrace, offset, pos);
            offset = pos;

            while (callerFrame != null) {
                InterpreterData idata = callerFrame.idata;
                sb.append(lineSeparator);
                sb.append("\tat script");
                if (idata.itsName != null && idata.itsName.length() != 0) {
                    sb.append('.');
                    sb.append(idata.itsName);
                }
                sb.append('(');
                sb.append(idata.itsSourceFile);
                int pc = calleeFrame == null ? callerFrame.pcSourceLineStart : calleeFrame.parentPC;
                if (pc >= 0) {
                    // Include line info only if available
                    sb.append(':');
                    sb.append(getIndex(idata.itsICode, pc));
                }
                sb.append(')');
                calleeFrame = callerFrame;
                callerFrame = callerFrame.parentFrame;
            }
            frame = calleeFrame.previousInterpreterFrame;
        }
        sb.append(nativeStackTrace.substring(offset));

        return sb.toString();
    }

    @Override
    public List<String> getScriptStack(RhinoException ex) {
        ScriptStackElement[][] stack = getScriptStackElements(ex);
        List<String> list = new ArrayList<>(stack.length);
        String lineSeparator = SecurityUtilities.getSystemProperty("line.separator");
        for (ScriptStackElement[] group : stack) {
            StringBuilder sb = new StringBuilder();
            for (ScriptStackElement elem : group) {
                elem.renderJavaStyle(sb);
                sb.append(lineSeparator);
            }
            list.add(sb.toString());
        }
        return list;
    }

    public ScriptStackElement[][] getScriptStackElements(RhinoException ex) {
        if (ex.interpreterStackInfo == null) {
            return null;
        }

        List<ScriptStackElement[]> list = new ArrayList<>();

        CallFrame calleeFrame = null;
        CallFrame frame = (CallFrame) ex.interpreterStackInfo;
        while (frame != null) {
            CallFrame callerFrame = frame;
            List<ScriptStackElement> group = new ArrayList<>();
            while (callerFrame != null) {
                InterpreterData idata = callerFrame.fnOrScript.idata;
                String fileName = idata.itsSourceFile;
                String functionName = null;
                int lineNumber = -1;
                int pc = calleeFrame == null ? callerFrame.pcSourceLineStart : calleeFrame.parentPC;
                if (pc >= 0) {
                    lineNumber = getIndex(idata.itsICode, pc);
                }
                if (idata.itsName != null && idata.itsName.length() != 0) {
                    functionName = idata.itsName;
                }
                calleeFrame = callerFrame;
                callerFrame = callerFrame.parentFrame;
                group.add(new ScriptStackElement(fileName, functionName, lineNumber));
            }
            list.add(group.toArray(new ScriptStackElement[0]));
            frame = calleeFrame.previousInterpreterFrame;
        }
        return list.toArray(new ScriptStackElement[list.size()][]);
    }

    static String getRawSource(InterpreterData idata) {
        if (idata.rawSource == null) {
            return null;
        }
        return idata.rawSource.substring(idata.rawSourceStart, idata.rawSourceEnd);
    }

    private static void initFunction(
            Context cx, Scriptable scope, InterpretedFunction parent, int index) {
        InterpretedFunction fn;
        fn = InterpretedFunction.createFunction(cx, scope, parent, index);
        ScriptRuntime.initFunction(
                cx, scope, fn, fn.idata.itsFunctionType, parent.idata.evalScriptFlag);
    }

    static Object interpret(
            InterpretedFunction ifun,
            Context cx,
            Scriptable scope,
            Scriptable thisObj,
            Object[] args) {
        if (!ScriptRuntime.hasTopCall(cx)) Kit.codeBug();

        if (cx.interpreterSecurityDomain != ifun.securityDomain) {
            Object savedDomain = cx.interpreterSecurityDomain;
            cx.interpreterSecurityDomain = ifun.securityDomain;
            try {
                return ifun.securityController.callWithDomain(
                        ifun.securityDomain, cx, ifun, scope, thisObj, args);
            } finally {
                cx.interpreterSecurityDomain = savedDomain;
            }
        }

        CallFrame frame =
                initFrame(
                        cx,
                        scope,
                        thisObj,
                        ifun.getHomeObject(),
                        args,
                        null,
                        null,
                        0,
                        args.length,
                        ifun,
                        null);
        frame.isContinuationsTopFrame = cx.isContinuationsTopCall;
        cx.isContinuationsTopCall = false;

        return interpretLoop(cx, frame, null);
    }

    static class GeneratorState {
        GeneratorState(int operation, Object value) {
            this.operation = operation;
            this.value = value;
        }

        int operation;
        Object value;
        RuntimeException returnedException;
    }

    public static Object resumeGenerator(
            Context cx, Scriptable scope, int operation, Object savedState, Object value) {
        CallFrame frame = (CallFrame) savedState;
        CallFrame activeFrame = frame.shallowCloneFrozen((CallFrame) cx.lastInterpreterFrame);
        try {
            GeneratorState generatorState = new GeneratorState(operation, value);
            if (operation == NativeGenerator.GENERATOR_CLOSE) {
                try {
                    return interpretLoop(cx, activeFrame, generatorState);
                } catch (RuntimeException e) {
                    // Only propagate exceptions other than closingException
                    if (e != value) throw e;
                }
                return Undefined.instance;
            }
            Object result = interpretLoop(cx, activeFrame, generatorState);
            if (generatorState.returnedException != null) throw generatorState.returnedException;
            return result;
        } finally {
            activeFrame.syncStateToFrame(frame);
        }
    }

    public static Object restartContinuation(
            NativeContinuation c, Context cx, Scriptable scope, Object[] args) {
        if (!ScriptRuntime.hasTopCall(cx)) {
            return ScriptRuntime.doTopCall(c, cx, scope, null, args, cx.isTopLevelStrict);
        }

        Object arg;
        if (args.length == 0) {
            arg = Undefined.instance;
        } else {
            arg = args[0];
        }

        CallFrame capturedFrame = (CallFrame) c.getImplementation();
        if (capturedFrame == null) {
            // No frames to restart
            return arg;
        }

        ContinuationJump cjump = new ContinuationJump(c, null);

        cjump.result = arg;
        return interpretLoop(cx, null, cjump);
    }

    // arbitrary number to add to instructionCount when calling
    // other functions
    private static final int INVOCATION_COST = 100;
    // arbitrary exception cost for instruction counting
    private static final int EXCEPTION_COST = 100;

    private static final Object undefined = Undefined.instance;

    private static class NewState {}

    // Null will be used to represent simple loop continuation in new
    // instructions, but we'll keep this type here while we move
    // things over.

    private static final class ContinueLoop extends NewState {
        private final int stackTop;
        private final int indexReg;

        private ContinueLoop(int stackTop, int indexReg) {
            this.stackTop = stackTop;
            this.indexReg = indexReg;
        }
    }

    private abstract static class InterpreterResult extends NewState {}

    private static class YieldResult extends InterpreterResult {
        private final Object yielding;

        private YieldResult(Object yielding) {
            this.yielding = yielding;
        }
    }

    private static class StateBreakResult extends InterpreterResult {
        private final CallFrame frame;

        private StateBreakResult(CallFrame frame) {
            this.frame = frame;
        }
    }

    private static class StateContinueResult extends InterpreterResult {
        private final CallFrame frame;
        private final int indexReg;

        private StateContinueResult(CallFrame frame, int indexReg) {
            this.frame = frame;
            this.indexReg = indexReg;
        }
    }

    private static class ThrowableResult extends InterpreterResult {
        private final CallFrame frame;
        private final Object throwable;

        private ThrowableResult(CallFrame frame, Object throwable) {
            this.frame = frame;
            this.throwable = throwable;
        }
    }

    private static final NewState BREAK_LOOP = new NewState() {};

    private static final NewState BREAK_JUMPLESSRUN = new NewState() {};

    private static final NewState BREAK_WITHOUT_EXTENSION = new NewState() {};

    private static class InterpreterState {
        int stackTop;
        int indexReg;
        BigInteger bigIntReg;
        String stringReg;
        final boolean instructionCounting;
        GeneratorState generatorState;
        Object throwable;

        InterpreterState(int stackTop, int indexReg, boolean instructionCounting) {
            this.stackTop = stackTop;
            this.indexReg = indexReg;
            this.instructionCounting = instructionCounting;
        }
    }

    private abstract static class InstructionClass {
        abstract NewState execute(Context cx, CallFrame frame, InterpreterState state, int op);
    }

    private static final InstructionClass[] instructionObjs;

    static {
        instructionObjs = new InstructionClass[Token.LAST_BYTECODE_TOKEN + 1 - MIN_ICODE];
        int base = -MIN_ICODE;
        instructionObjs[base + Icode_REG_STR_C0] = new DoStringCn();
        instructionObjs[base + Icode_REG_STR_C1] = new DoStringCn();
        instructionObjs[base + Icode_REG_STR_C2] = new DoStringCn();
        instructionObjs[base + Icode_REG_STR_C3] = new DoStringCn();
        instructionObjs[base + Icode_REG_STR1] = new DoRegString1();
        instructionObjs[base + Icode_REG_STR2] = new DoRegString2();
        instructionObjs[base + Icode_REG_STR4] = new DoRegString4();
        instructionObjs[base + Icode_REG_BIGINT_C0] = new DoBigIntCn();
        instructionObjs[base + Icode_REG_BIGINT_C1] = new DoBigIntCn();
        instructionObjs[base + Icode_REG_BIGINT_C2] = new DoBigIntCn();
        instructionObjs[base + Icode_REG_BIGINT_C3] = new DoBigIntCn();
        instructionObjs[base + Icode_REG_BIGINT1] = new DoRegBigInt1();
        instructionObjs[base + Icode_REG_BIGINT2] = new DoRegBigInt2();
        instructionObjs[base + Icode_REG_BIGINT4] = new DoRegBigInt4();
    }

    private static Object interpretLoop(Context cx, CallFrame frame, Object throwable) {
        final Object oldFrame = cx.lastInterpreterFrame;
        try {
            // throwable holds exception object to rethrow or catch
            // It is also used for continuation restart in which case
            // it holds ContinuationJump

            final boolean instructionCounting = cx.instructionThreshold != 0;

            String stringReg = null;
            BigInteger bigIntReg = null;
            int indexReg = -1;

            // When restarting continuation throwable is not null and to jump
            // to the code that rewind continuation state indexReg should be set
            // to -1.
            // With the normal call throwable == null and indexReg == -1 allows to
            // catch bugs with using indeReg to access array elements before
            // initializing indexReg.

            GeneratorState generatorState = null;
            if (throwable != null) {
                if (throwable instanceof GeneratorState) {
                    generatorState = (GeneratorState) throwable;

                    // reestablish this call frame
                    enterFrame(cx, frame, ScriptRuntime.emptyArgs, true);
                    throwable = null;
                } else if (!(throwable instanceof ContinuationJump)) {
                    // It should be continuation
                    Kit.codeBug();
                }
            }

            Object interpreterResult = null;
            double interpreterResultDbl = 0.0;

            StateLoop:
            for (; ; ) {

                Withoutexceptions:
                try {

                    if (throwable != null) {
                        // Need to return both 'frame' and 'throwable' from
                        // 'processThrowable', so just added a 'throwable'
                        // member in 'frame'.
                        frame =
                                processThrowable(
                                        cx, throwable, frame, indexReg, instructionCounting);
                        throwable = frame.throwable;
                        frame.throwable = null;
                    } else {
                        if (generatorState == null && frame.frozen) Kit.codeBug();
                    }

                    InterpreterResult result =
                            interpretFunction(
                                    cx,
                                    frame,
                                    throwable,
                                    generatorState,
                                    indexReg,
                                    instructionCounting);
                    if (result instanceof StateContinueResult) {
                        StateContinueResult scr = (StateContinueResult) result;
                        frame = scr.frame;
                        indexReg = scr.indexReg;
                        continue StateLoop;
                    } else if (result instanceof StateBreakResult) {
                        StateBreakResult sbr = (StateBreakResult) result;
                        frame = sbr.frame;
                        interpreterResult = frame.result;
                        interpreterResultDbl = frame.resultDbl;
                        break StateLoop;
                    } else if (result instanceof YieldResult) {
                        return ((YieldResult) result).yielding;
                    } else if (result instanceof ThrowableResult) {
                        ThrowableResult tr = (ThrowableResult) result;
                        frame = tr.frame;
                        throwable = tr.throwable;
                        break Withoutexceptions;
                    } else {
                        Kit.codeBug();
                    }
                } // end of interpreter withoutExceptions: try
                catch (Throwable ex) {
                    if (throwable != null) {
                        // This is serious bug and it is better to track it ASAP
                        ex.printStackTrace(System.err);
                        throw new IllegalStateException();
                    }
                    throwable = ex;
                }

                // This should be reachable only after above catch or from
                // finally when it needs to propagate exception or from
                // explicit throw
                if (throwable == null) Kit.codeBug();

                // Exception type
                final int EX_CATCH_STATE = 2; // Can execute JS catch
                final int EX_FINALLY_STATE = 1; // Can execute JS finally
                final int EX_NO_JS_STATE = 0; // Terminate JS execution

                int exState;
                ContinuationJump cjump = null;

                if (generatorState != null
                        && generatorState.operation == NativeGenerator.GENERATOR_CLOSE
                        && throwable == generatorState.value) {
                    exState = EX_FINALLY_STATE;
                } else if (throwable instanceof JavaScriptException) {
                    exState = EX_CATCH_STATE;
                } else if (throwable instanceof EcmaError) {
                    // an offical ECMA error object,
                    exState = EX_CATCH_STATE;
                } else if (throwable instanceof EvaluatorException) {
                    exState = EX_CATCH_STATE;
                } else if (throwable instanceof ContinuationPending) {
                    exState = EX_NO_JS_STATE;
                } else if (throwable instanceof RuntimeException) {
                    exState =
                            cx.hasFeature(Context.FEATURE_ENHANCED_JAVA_ACCESS)
                                    ? EX_CATCH_STATE
                                    : EX_FINALLY_STATE;
                } else if (throwable instanceof Error) {
                    exState =
                            cx.hasFeature(Context.FEATURE_ENHANCED_JAVA_ACCESS)
                                    ? EX_CATCH_STATE
                                    : EX_NO_JS_STATE;
                } else if (throwable instanceof ContinuationJump) {
                    // It must be ContinuationJump
                    exState = EX_FINALLY_STATE;
                    cjump = (ContinuationJump) throwable;
                } else {
                    exState =
                            cx.hasFeature(Context.FEATURE_ENHANCED_JAVA_ACCESS)
                                    ? EX_CATCH_STATE
                                    : EX_FINALLY_STATE;
                }

                if (instructionCounting) {
                    try {
                        addInstructionCount(cx, frame, EXCEPTION_COST);
                    } catch (RuntimeException ex) {
                        throwable = ex;
                        exState = EX_FINALLY_STATE;
                    } catch (Error ex) {
                        // Error from instruction counting
                        //     => unconditionally terminate JS
                        throwable = ex;
                        cjump = null;
                        exState = EX_NO_JS_STATE;
                    }
                }
                if (frame.debuggerFrame != null && throwable instanceof RuntimeException) {
                    // Call debugger only for RuntimeException
                    RuntimeException rex = (RuntimeException) throwable;
                    try {
                        frame.debuggerFrame.onExceptionThrown(cx, rex);
                    } catch (Throwable ex) {
                        // Any exception from debugger
                        //     => unconditionally terminate JS
                        throwable = ex;
                        cjump = null;
                        exState = EX_NO_JS_STATE;
                    }
                }

                for (; ; ) {
                    if (exState != EX_NO_JS_STATE) {
                        boolean onlyFinally = (exState != EX_CATCH_STATE);
                        indexReg = getExceptionHandler(frame, onlyFinally);
                        if (indexReg >= 0) {
                            // We caught an exception, restart the loop
                            // with exception pending the processing at the loop
                            // start
                            continue StateLoop;
                        }
                    }
                    // No allowed exception handlers in this frame, unwind
                    // to parent and try to look there

                    exitFrame(cx, frame, throwable);

                    frame = frame.parentFrame;
                    if (frame == null) {
                        break;
                    }
                    if (cjump != null && Objects.equals(cjump.branchFrame, frame)) {
                        // Continuation branch point was hit,
                        // restart the state loop to reenter continuation
                        indexReg = -1;
                        continue StateLoop;
                    }
                }

                // No more frames, rethrow the exception or deal with continuation
                if (cjump != null) {
                    if (cjump.branchFrame != null) {
                        // The above loop should locate the top frame
                        Kit.codeBug();
                    }
                    if (cjump.capturedFrame != null) {
                        // Restarting detached continuation
                        indexReg = -1;
                        continue StateLoop;
                    }
                    // Return continuation result to the caller
                    interpreterResult = cjump.result;
                    interpreterResultDbl = cjump.resultDbl;
                    throwable = null;
                }
                break StateLoop;
            } // end of StateLoop: for(;;)

            // Do cleanups/restorations before the final return or throw

            if (frame != null) {
                cx.lastInterpreterFrame =
                        frame.parentFrame == null
                                ? frame.previousInterpreterFrame
                                : frame.parentFrame;
            } else {
                cx.lastInterpreterFrame = null;
            }

            if (throwable != null) {
                if (throwable instanceof RuntimeException) {
                    throw (RuntimeException) throwable;
                }
                // Must be instance of Error or code bug
                throw (Error) throwable;
            }

            return (interpreterResult != DBL_MRK)
                    ? interpreterResult
                    : ScriptRuntime.wrapNumber(interpreterResultDbl);
        } finally {
            cx.lastInterpreterFrame = oldFrame;
        }
    }

    private static final Object DBL_MRK = DOUBLE_MARK;

    private static InterpreterResult interpretFunction(
            Context cx,
            CallFrame frame,
            Object throwable,
            GeneratorState generatorState,
            int indexReg,
            boolean instructionCounting) {

        withoutExceptions:
        try {

            // Use local variables for constant values in frame
            // for faster access
            final Object[] stack = frame.stack;
            final double[] sDbl = frame.sDbl;
            final Object[] vars = frame.varSource.stack;
            final double[] varDbls = frame.varSource.sDbl;
            final byte[] varAttributes = frame.varSource.stackAttributes;
            final InterpreterData iData = frame.idata;
            final byte[] iCode = frame.idata.itsICode;
            final String[] strings = frame.idata.itsStringTable;
            final BigInteger[] bigInts = frame.idata.itsBigIntTable;
            String stringReg = null;
            BigInteger bigIntReg = null;

            // Use local for stackTop as well. Since execption handlers
            // can only exist at statement level where stack is empty,
            // it is necessary to save/restore stackTop only across
            // function calls and normal returns.
            int stackTop = frame.savedStackTop;

            // Store new frame in cx which is used for error reporting etc.
            cx.lastInterpreterFrame = frame;

            Loop:
            for (; ; ) {

                // Exception handler assumes that PC is already incremented
                // pass the instruction start when it searches the
                // exception handler
                int op = iCode[frame.pc++];
                jumplessRun:
                {
                    { // Extra braces to reduce the formatting change.
                        // Back indent to ease implementation reading
                        switch (op) {
                            case Icode_GENERATOR:
                                {
                                    if (!frame.frozen) {
                                        // First time encountering this opcode: create new generator
                                        // object and return
                                        frame.pc--; // we want to come back here when we resume
                                        CallFrame generatorFrame = captureFrameForGenerator(frame);
                                        generatorFrame.frozen = true;
                                        if (cx.getLanguageVersion() >= Context.VERSION_ES6) {
                                            frame.result =
                                                    new ES6Generator(
                                                            frame.scope,
                                                            generatorFrame.fnOrScript,
                                                            generatorFrame);
                                        } else {
                                            frame.result =
                                                    new NativeGenerator(
                                                            frame.scope,
                                                            generatorFrame.fnOrScript,
                                                            generatorFrame);
                                        }
                                        break Loop;
                                    }
                                    // We are now resuming execution. Fall through to YIELD case.
                                }
                            // fall through...
                            case Token.YIELD:
                            case Icode_YIELD_STAR:
                                {
                                    /* This is both where we yield
                                     * from and re-enter the
                                     * generator. */
                                    if (!frame.frozen) {
                                        return new YieldResult(
                                                freezeGenerator(
                                                        cx,
                                                        frame,
                                                        stackTop,
                                                        generatorState,
                                                        op == Icode_YIELD_STAR));
                                    }
                                    Object obj = thawGenerator(frame, stackTop, generatorState, op);
                                    if (obj != Scriptable.NOT_FOUND) {
                                        throwable = obj;
                                        break withoutExceptions;
                                    }
                                    continue Loop;
                                }
                            case Icode_GENERATOR_END:
                                {
                                    // throw StopIteration
                                    frame.frozen = true;
                                    int sourceLine = getIndex(iCode, frame.pc);
                                    generatorState.returnedException =
                                            new JavaScriptException(
                                                    NativeIterator.getStopIterationObject(
                                                            frame.scope),
                                                    frame.idata.itsSourceFile,
                                                    sourceLine);
                                    break Loop;
                                }
                            case Icode_GENERATOR_RETURN:
                                {
                                    // throw StopIteration with the value of "return"
                                    frame.frozen = true;
                                    frame.result = stack[stackTop];
                                    frame.resultDbl = sDbl[stackTop];
                                    --stackTop;

                                    NativeIterator.StopIteration si =
                                            new NativeIterator.StopIteration(
                                                    (frame.result == DOUBLE_MARK)
                                                            ? Double.valueOf(frame.resultDbl)
                                                            : frame.result);

                                    int sourceLine = getIndex(iCode, frame.pc);
                                    generatorState.returnedException =
                                            new JavaScriptException(
                                                    si, iData.itsSourceFile, sourceLine);
                                    break Loop;
                                }
                            case Token.THROW:
                                {
                                    Object value = stack[stackTop];
                                    if (value == DOUBLE_MARK)
                                        value = ScriptRuntime.wrapNumber(sDbl[stackTop]);
                                    --stackTop;

                                    int sourceLine = getIndex(iCode, frame.pc);
                                    throwable =
                                            new JavaScriptException(
                                                    value, iData.itsSourceFile, sourceLine);
                                    break withoutExceptions;
                                }
                            case Token.RETHROW:
                                {
                                    indexReg += iData.itsMaxVars;
                                    throwable = stack[indexReg];
                                    break withoutExceptions;
                                }
                            case Token.GE:
                            case Token.LE:
                            case Token.GT:
                            case Token.LT:
                                {
                                    stackTop = doCompare(frame, op, stack, sDbl, stackTop);
                                    continue Loop;
                                }
                            case Token.IN:
                            case Token.INSTANCEOF:
                                {
                                    stackTop = doInOrInstanceof(cx, op, stack, sDbl, stackTop);
                                    continue Loop;
                                }
                            case Token.EQ:
                            case Token.NE:
                                {
                                    --stackTop;
                                    boolean valBln = doEquals(stack, sDbl, stackTop);
                                    valBln ^= (op == Token.NE);
                                    stack[stackTop] = ScriptRuntime.wrapBoolean(valBln);
                                    continue Loop;
                                }
                            case Token.SHEQ:
                            case Token.SHNE:
                                {
                                    --stackTop;
                                    boolean valBln = doShallowEquals(stack, sDbl, stackTop);
                                    valBln ^= (op == Token.SHNE);
                                    stack[stackTop] = ScriptRuntime.wrapBoolean(valBln);
                                    continue Loop;
                                }
                            case Token.IFNE:
                                if (stack_boolean(frame, stackTop--)) {
                                    frame.pc += 2;
                                    continue Loop;
                                }
                                break jumplessRun;
                            case Token.IFEQ:
                                if (!stack_boolean(frame, stackTop--)) {
                                    frame.pc += 2;
                                    continue Loop;
                                }
                                break jumplessRun;
                            case Icode_IFEQ_POP:
                                if (!stack_boolean(frame, stackTop--)) {
                                    frame.pc += 2;
                                    continue Loop;
                                }
                                stack[stackTop--] = null;
                                break jumplessRun;
                            case Icode_IF_NULL_UNDEF:
                                {
                                    Object val = frame.stack[stackTop];
                                    --stackTop;
                                    if (val != null && !Undefined.isUndefined(val)) {
                                        frame.pc += 2;
                                        continue Loop;
                                    }
                                    break jumplessRun;
                                }
                            case Icode_IF_NOT_NULL_UNDEF:
                                {
                                    Object val = frame.stack[stackTop];
                                    --stackTop;
                                    if (val == null || Undefined.isUndefined(val)) {
                                        frame.pc += 2;
                                        continue Loop;
                                    }
                                    break jumplessRun;
                                }
                            case Token.GOTO:
                                break jumplessRun;
                            case Icode_GOSUB:
                                ++stackTop;
                                stack[stackTop] = DOUBLE_MARK;
                                sDbl[stackTop] = frame.pc + 2;
                                break jumplessRun;
                            case Icode_STARTSUB:
                                if (stackTop == frame.emptyStackTop + 1) {
                                    // Call from Icode_GOSUB: store return PC address in the local
                                    indexReg += iData.itsMaxVars;
                                    stack[indexReg] = stack[stackTop];
                                    sDbl[indexReg] = sDbl[stackTop];
                                    --stackTop;
                                } else {
                                    // Call from exception handler: exception object is already
                                    // stored
                                    // in the local
                                    if (stackTop != frame.emptyStackTop) Kit.codeBug();
                                }
                                continue Loop;
                            case Icode_RETSUB:
                                {
                                    // indexReg: local to store return address
                                    if (instructionCounting) {
                                        addInstructionCount(cx, frame, 0);
                                    }
                                    indexReg += iData.itsMaxVars;
                                    Object value = stack[indexReg];
                                    if (value != DOUBLE_MARK) {
                                        // Invocation from exception handler, restore object to
                                        // rethrow
                                        throwable = value;
                                        break withoutExceptions;
                                    }
                                    // Normal return from GOSUB
                                    frame.pc = (int) sDbl[indexReg];
                                    if (instructionCounting) {
                                        frame.pcPrevBranch = frame.pc;
                                    }
                                    continue Loop;
                                }
                            case Icode_POP:
                                stack[stackTop] = null;
                                stackTop--;
                                continue Loop;
                            case Icode_POP_RESULT:
                                frame.result = stack[stackTop];
                                frame.resultDbl = sDbl[stackTop];
                                stack[stackTop] = null;
                                --stackTop;
                                continue Loop;
                            case Icode_DUP:
                                stack[stackTop + 1] = stack[stackTop];
                                sDbl[stackTop + 1] = sDbl[stackTop];
                                stackTop++;
                                continue Loop;
                            case Icode_DUP2:
                                stack[stackTop + 1] = stack[stackTop - 1];
                                sDbl[stackTop + 1] = sDbl[stackTop - 1];
                                stack[stackTop + 2] = stack[stackTop];
                                sDbl[stackTop + 2] = sDbl[stackTop];
                                stackTop += 2;
                                continue Loop;
                            case Icode_SWAP:
                                {
                                    Object o = stack[stackTop];
                                    stack[stackTop] = stack[stackTop - 1];
                                    stack[stackTop - 1] = o;
                                    double d = sDbl[stackTop];
                                    sDbl[stackTop] = sDbl[stackTop - 1];
                                    sDbl[stackTop - 1] = d;
                                    continue Loop;
                                }
                            case Token.RETURN:
                                frame.result = stack[stackTop];
                                frame.resultDbl = sDbl[stackTop];
                                --stackTop;
                                break Loop;
                            case Token.RETURN_RESULT:
                                break Loop;
                            case Icode_RETUNDEF:
                                frame.result = undefined;
                                break Loop;
                            case Token.BITNOT:
                                {
                                    stackTop = doBitNOT(frame, stack, sDbl, stackTop);
                                    continue Loop;
                                }
                            case Token.BITAND:
                            case Token.BITOR:
                            case Token.BITXOR:
                            case Token.LSH:
                            case Token.RSH:
                                {
                                    stackTop = doBitOp(frame, op, stack, sDbl, stackTop);
                                    continue Loop;
                                }
                            case Token.URSH:
                                {
                                    double lDbl = stack_double(frame, stackTop - 1);
                                    int rIntValue = stack_int32(frame, stackTop) & 0x1F;
                                    stack[--stackTop] = DOUBLE_MARK;
                                    sDbl[stackTop] = ScriptRuntime.toUint32(lDbl) >>> rIntValue;
                                    continue Loop;
                                }
                            case Token.POS:
                                {
                                    double rDbl = stack_double(frame, stackTop);
                                    stack[stackTop] = DOUBLE_MARK;
                                    sDbl[stackTop] = rDbl;
                                    continue Loop;
                                }
                            case Token.NEG:
                                {
                                    Number rNum = stack_numeric(frame, stackTop);
                                    Number rNegNum = ScriptRuntime.negate(rNum);
                                    if (rNegNum instanceof BigInteger) {
                                        stack[stackTop] = rNegNum;
                                    } else {
                                        stack[stackTop] = DOUBLE_MARK;
                                        sDbl[stackTop] = rNegNum.doubleValue();
                                    }
                                    continue Loop;
                                }
                            case Token.ADD:
                                --stackTop;
                                doAdd(stack, sDbl, stackTop, cx);
                                continue Loop;
                            case Token.SUB:
                            case Token.MUL:
                            case Token.DIV:
                            case Token.MOD:
                            case Token.EXP:
                                {
                                    stackTop = doArithmetic(frame, op, stack, sDbl, stackTop);
                                    continue Loop;
                                }
                            case Token.NOT:
                                stack[stackTop] =
                                        ScriptRuntime.wrapBoolean(!stack_boolean(frame, stackTop));
                                continue Loop;
                            case Token.BINDNAME:
                                stack[++stackTop] = ScriptRuntime.bind(cx, frame.scope, stringReg);
                                continue Loop;
                            case Token.STRICT_SETNAME:
                            case Token.SETNAME:
                                {
                                    Object rhs = stack[stackTop];
                                    if (rhs == DOUBLE_MARK)
                                        rhs = ScriptRuntime.wrapNumber(sDbl[stackTop]);
                                    --stackTop;
                                    Scriptable lhs = (Scriptable) stack[stackTop];
                                    stack[stackTop] =
                                            op == Token.SETNAME
                                                    ? ScriptRuntime.setName(
                                                            lhs, rhs, cx, frame.scope, stringReg)
                                                    : ScriptRuntime.strictSetName(
                                                            lhs, rhs, cx, frame.scope, stringReg);
                                    continue Loop;
                                }
                            case Icode_SETCONST:
                                {
                                    Object rhs = stack[stackTop];
                                    if (rhs == DOUBLE_MARK)
                                        rhs = ScriptRuntime.wrapNumber(sDbl[stackTop]);
                                    --stackTop;
                                    Scriptable lhs = (Scriptable) stack[stackTop];
                                    stack[stackTop] =
                                            ScriptRuntime.setConst(lhs, rhs, cx, stringReg);
                                    continue Loop;
                                }
                            case Token.DELPROP:
                            case Icode_DELNAME:
                                {
                                    stackTop = doDelName(cx, frame, op, stack, sDbl, stackTop);
                                    continue Loop;
                                }
                            case Icode_DELPROP_SUPER:
                                stackTop -= 1;
                                stack[stackTop] = Boolean.FALSE;
                                ScriptRuntime.throwDeleteOnSuperPropertyNotAllowed();
                                continue Loop;
                            case Token.GETPROPNOWARN:
                                {
                                    Object lhs = stack[stackTop];
                                    if (lhs == DOUBLE_MARK)
                                        lhs = ScriptRuntime.wrapNumber(sDbl[stackTop]);
                                    stack[stackTop] =
                                            ScriptRuntime.getObjectPropNoWarn(
                                                    lhs, stringReg, cx, frame.scope);
                                    continue Loop;
                                }
                            case Token.GETPROP:
                                {
                                    Object lhs = stack[stackTop];
                                    if (lhs == DOUBLE_MARK)
                                        lhs = ScriptRuntime.wrapNumber(sDbl[stackTop]);
                                    stack[stackTop] =
                                            ScriptRuntime.getObjectProp(
                                                    lhs, stringReg, cx, frame.scope);
                                    continue Loop;
                                }
                            case Token.GETPROP_SUPER:
                            case Token.GETPROPNOWARN_SUPER:
                                {
                                    Object superObject = stack[stackTop];
                                    if (superObject == DOUBLE_MARK) Kit.codeBug();
                                    stack[stackTop] =
                                            ScriptRuntime.getSuperProp(
                                                    superObject,
                                                    stringReg,
                                                    cx,
                                                    frame.scope,
                                                    frame.thisObj,
                                                    op == Token.GETPROPNOWARN_SUPER);
                                    continue Loop;
                                }
                            case Token.SETPROP:
                                {
                                    Object rhs = stack[stackTop];
                                    if (rhs == DOUBLE_MARK)
                                        rhs = ScriptRuntime.wrapNumber(sDbl[stackTop]);
                                    --stackTop;
                                    Object lhs = stack[stackTop];
                                    if (lhs == DOUBLE_MARK)
                                        lhs = ScriptRuntime.wrapNumber(sDbl[stackTop]);
                                    stack[stackTop] =
                                            ScriptRuntime.setObjectProp(
                                                    lhs, stringReg, rhs, cx, frame.scope);
                                    continue Loop;
                                }
                            case Token.SETPROP_SUPER:
                                {
                                    Object rhs = stack[stackTop];
                                    if (rhs == DOUBLE_MARK)
                                        rhs = ScriptRuntime.wrapNumber(sDbl[stackTop]);
                                    --stackTop;
                                    Object superObject = stack[stackTop];
                                    if (superObject == DOUBLE_MARK) Kit.codeBug();
                                    stack[stackTop] =
                                            ScriptRuntime.setSuperProp(
                                                    superObject,
                                                    stringReg,
                                                    rhs,
                                                    cx,
                                                    frame.scope,
                                                    frame.thisObj);
                                    continue Loop;
                                }
                            case Icode_PROP_INC_DEC:
                                {
                                    Object lhs = stack[stackTop];
                                    if (lhs == DOUBLE_MARK)
                                        lhs = ScriptRuntime.wrapNumber(sDbl[stackTop]);
                                    stack[stackTop] =
                                            ScriptRuntime.propIncrDecr(
                                                    lhs,
                                                    stringReg,
                                                    cx,
                                                    frame.scope,
                                                    iCode[frame.pc]);
                                    ++frame.pc;
                                    continue Loop;
                                }
                            case Token.GETELEM:
                                {
                                    stackTop = doGetElem(cx, frame, stack, sDbl, stackTop);
                                    continue Loop;
                                }
                            case Token.GETELEM_SUPER:
                                {
                                    stackTop = doGetElemSuper(cx, frame, stack, sDbl, stackTop);
                                    continue Loop;
                                }
                            case Token.SETELEM:
                                {
                                    stackTop = doSetElem(cx, frame, stack, sDbl, stackTop);
                                    continue Loop;
                                }
                            case Token.SETELEM_SUPER:
                                {
                                    stackTop = doSetElemSuper(cx, frame, stack, sDbl, stackTop);
                                    continue Loop;
                                }
                            case Icode_ELEM_INC_DEC:
                                {
                                    stackTop =
                                            doElemIncDec(cx, frame, iCode, stack, sDbl, stackTop);
                                    continue Loop;
                                }
                            case Token.GET_REF:
                                {
                                    Ref ref = (Ref) stack[stackTop];
                                    stack[stackTop] = ScriptRuntime.refGet(ref, cx);
                                    continue Loop;
                                }
                            case Token.SET_REF:
                                {
                                    Object value = stack[stackTop];
                                    if (value == DOUBLE_MARK)
                                        value = ScriptRuntime.wrapNumber(sDbl[stackTop]);
                                    --stackTop;
                                    Ref ref = (Ref) stack[stackTop];
                                    stack[stackTop] =
                                            ScriptRuntime.refSet(ref, value, cx, frame.scope);
                                    continue Loop;
                                }
                            case Token.DEL_REF:
                                {
                                    Ref ref = (Ref) stack[stackTop];
                                    stack[stackTop] = ScriptRuntime.refDel(ref, cx);
                                    continue Loop;
                                }
                            case Icode_REF_INC_DEC:
                                {
                                    Ref ref = (Ref) stack[stackTop];
                                    stack[stackTop] =
                                            ScriptRuntime.refIncrDecr(
                                                    ref, cx, frame.scope, iCode[frame.pc]);
                                    ++frame.pc;
                                    continue Loop;
                                }
                            case Token.LOCAL_LOAD:
                                ++stackTop;
                                indexReg += iData.itsMaxVars;
                                stack[stackTop] = stack[indexReg];
                                sDbl[stackTop] = sDbl[indexReg];
                                continue Loop;
                            case Icode_LOCAL_CLEAR:
                                indexReg += iData.itsMaxVars;
                                stack[indexReg] = null;
                                continue Loop;
                            case Icode_NAME_AND_THIS:
                                // stringReg: name
                                ++stackTop;
                                stack[stackTop] =
                                        ScriptRuntime.getNameAndThis(stringReg, cx, frame.scope);
                                continue Loop;
                            case Icode_NAME_AND_THIS_OPTIONAL:
                                // stringReg: name
                                ++stackTop;
                                stack[stackTop] =
                                        ScriptRuntime.getNameAndThisOptional(
                                                stringReg, cx, frame.scope);
                                continue Loop;
                            case Icode_PROP_AND_THIS:
                                {
                                    Object obj = stack[stackTop];
                                    if (obj == DOUBLE_MARK)
                                        obj = ScriptRuntime.wrapNumber(sDbl[stackTop]);
                                    // stringReg: property
                                    stack[stackTop] =
                                            ScriptRuntime.getPropAndThis(
                                                    obj, stringReg, cx, frame.scope);
                                    continue Loop;
                                }
                            case Icode_PROP_AND_THIS_OPTIONAL:
                                {
                                    Object obj = stack[stackTop];
                                    if (obj == DOUBLE_MARK)
                                        obj = ScriptRuntime.wrapNumber(sDbl[stackTop]);
                                    // stringReg: property
                                    stack[stackTop] =
                                            ScriptRuntime.getPropAndThisOptional(
                                                    obj, stringReg, cx, frame.scope);
                                    continue Loop;
                                }
                            case Icode_ELEM_AND_THIS:
                                {
                                    Object obj = stack[stackTop - 1];
                                    if (obj == DOUBLE_MARK)
                                        obj = ScriptRuntime.wrapNumber(sDbl[stackTop - 1]);
                                    Object id = stack[stackTop];
                                    if (id == DOUBLE_MARK)
                                        id = ScriptRuntime.wrapNumber(sDbl[stackTop]);
                                    stackTop--;
                                    stack[stackTop] =
                                            ScriptRuntime.getElemAndThis(obj, id, cx, frame.scope);
                                    continue Loop;
                                }
                            case Icode_ELEM_AND_THIS_OPTIONAL:
                                {
                                    Object obj = stack[stackTop - 1];
                                    if (obj == DOUBLE_MARK)
                                        obj = ScriptRuntime.wrapNumber(sDbl[stackTop - 1]);
                                    Object id = stack[stackTop];
                                    if (id == DOUBLE_MARK)
                                        id = ScriptRuntime.wrapNumber(sDbl[stackTop]);
                                    stackTop--;
                                    stack[stackTop] =
                                            ScriptRuntime.getElemAndThisOptional(
                                                    obj, id, cx, frame.scope);
                                    continue Loop;
                                }
                            case Icode_VALUE_AND_THIS:
                                {
                                    Object value = stack[stackTop];
                                    if (value == DOUBLE_MARK)
                                        value = ScriptRuntime.wrapNumber(sDbl[stackTop]);
                                    stack[stackTop] = ScriptRuntime.getValueAndThis(value, cx);
                                    continue Loop;
                                }
                            case Icode_VALUE_AND_THIS_OPTIONAL:
                                {
                                    Object value = stack[stackTop];
                                    if (value == DOUBLE_MARK)
                                        value = ScriptRuntime.wrapNumber(sDbl[stackTop]);
                                    stack[stackTop] =
                                            ScriptRuntime.getValueAndThisOptional(value, cx);
                                    continue Loop;
                                }
                            case Icode_CALLSPECIAL:
                                {
                                    if (instructionCounting) {
                                        cx.instructionCount += INVOCATION_COST;
                                    }
                                    stackTop =
                                            doCallSpecial(
                                                    cx, frame, stack, sDbl, stackTop, iCode,
                                                    indexReg, false);
                                    continue Loop;
                                }
                            case Icode_CALLSPECIAL_OPTIONAL:
                                {
                                    if (instructionCounting) {
                                        cx.instructionCount += INVOCATION_COST;
                                    }
                                    stackTop =
                                            doCallSpecial(
                                                    cx, frame, stack, sDbl, stackTop, iCode,
                                                    indexReg, true);
                                    continue Loop;
                                }
                            case Token.CALL:
                            case Icode_CALL_ON_SUPER:
                            case Icode_TAIL_CALL:
                            case Token.REF_CALL:
                                {
                                    var callState =
                                            doCallByteCode(
                                                    cx,
                                                    frame,
                                                    instructionCounting,
                                                    op,
                                                    stackTop,
                                                    indexReg);
                                    if (callState instanceof ContinueLoop) {
                                        var contLoop = (ContinueLoop) callState;
                                        stackTop = contLoop.stackTop;
                                        indexReg = contLoop.indexReg;
                                        continue Loop;
                                    } else if (callState instanceof StateContinueResult) {
                                        return (StateContinueResult) callState;
                                    } else if (callState instanceof ThrowableResult) {
                                        throwable = ((ThrowableResult) callState).throwable;
                                        break withoutExceptions;
                                    } else {
                                        Kit.codeBug();
                                        break;
                                    }
                                }
                            case Token.NEW:
                                {
                                    if (instructionCounting) {
                                        cx.instructionCount += INVOCATION_COST;
                                    }
                                    // stack change: function arg0 .. argN -> newResult
                                    // indexReg: number of arguments
                                    stackTop -= indexReg;

                                    Object lhs = stack[stackTop];
                                    if (lhs instanceof InterpretedFunction) {
                                        InterpretedFunction f = (InterpretedFunction) lhs;
                                        if (frame.fnOrScript.securityDomain == f.securityDomain) {
                                            if (cx.getLanguageVersion() >= Context.VERSION_ES6
                                                    && f.getHomeObject() != null) {
                                                // Only methods have home objects associated with
                                                // them
                                                throw ScriptRuntime.typeErrorById(
                                                        "msg.not.ctor", f.getFunctionName());
                                            }

                                            Scriptable newInstance =
                                                    f.createObject(cx, frame.scope);
                                            CallFrame calleeFrame =
                                                    initFrame(
                                                            cx,
                                                            frame.scope,
                                                            newInstance,
                                                            newInstance,
                                                            stack,
                                                            sDbl,
                                                            null,
                                                            stackTop + 1,
                                                            indexReg,
                                                            f,
                                                            frame);

                                            stack[stackTop] = newInstance;
                                            frame.savedStackTop = stackTop;
                                            frame.savedCallOp = op;
                                            return new StateContinueResult(calleeFrame, indexReg);
                                        }
                                    }
                                    if (!(lhs instanceof Constructable)) {
                                        if (lhs == DOUBLE_MARK)
                                            lhs = ScriptRuntime.wrapNumber(sDbl[stackTop]);
                                        throw ScriptRuntime.notFunctionError(lhs);
                                    }
                                    Constructable ctor = (Constructable) lhs;

                                    if (ctor instanceof IdFunctionObject) {
                                        IdFunctionObject ifun = (IdFunctionObject) ctor;
                                        if (NativeContinuation.isContinuationConstructor(ifun)) {
                                            frame.stack[stackTop] =
                                                    captureContinuation(
                                                            cx, frame.parentFrame, false);
                                            continue Loop;
                                        }
                                    }

                                    Object[] outArgs =
                                            getArgsArray(stack, sDbl, stackTop + 1, indexReg);
                                    stack[stackTop] = ctor.construct(cx, frame.scope, outArgs);
                                    continue Loop;
                                }
                            case Token.TYPEOF:
                                {
                                    Object lhs = stack[stackTop];
                                    if (lhs == DOUBLE_MARK)
                                        lhs = ScriptRuntime.wrapNumber(sDbl[stackTop]);
                                    stack[stackTop] = ScriptRuntime.typeof(lhs);
                                    continue Loop;
                                }
                            case Icode_TYPEOFNAME:
                                stack[++stackTop] =
                                        ScriptRuntime.typeofName(frame.scope, stringReg);
                                continue Loop;
                            case Token.STRING:
                                stack[++stackTop] = stringReg;
                                continue Loop;
                            case Icode_SHORTNUMBER:
                                ++stackTop;
                                stack[stackTop] = DOUBLE_MARK;
                                sDbl[stackTop] = getShort(iCode, frame.pc);
                                frame.pc += 2;
                                continue Loop;
                            case Icode_INTNUMBER:
                                ++stackTop;
                                stack[stackTop] = DOUBLE_MARK;
                                sDbl[stackTop] = getInt(iCode, frame.pc);
                                frame.pc += 4;
                                continue Loop;
                            case Token.NUMBER:
                                ++stackTop;
                                stack[stackTop] = DOUBLE_MARK;
                                sDbl[stackTop] = iData.itsDoubleTable[indexReg];
                                continue Loop;
                            case Token.BIGINT:
                                stack[++stackTop] = bigIntReg;
                                continue Loop;
                            case Token.NAME:
                                stack[++stackTop] = ScriptRuntime.name(cx, frame.scope, stringReg);
                                continue Loop;
                            case Icode_NAME_INC_DEC:
                                stack[++stackTop] =
                                        ScriptRuntime.nameIncrDecr(
                                                frame.scope, stringReg, cx, iCode[frame.pc]);
                                ++frame.pc;
                                continue Loop;
                            case Icode_SETCONSTVAR1:
                                indexReg = iCode[frame.pc++];
                            // fallthrough
                            case Icode_SETCONSTVAR:
                                stackTop =
                                        doSetConstVar(
                                                frame,
                                                stack,
                                                sDbl,
                                                stackTop,
                                                vars,
                                                varDbls,
                                                varAttributes,
                                                indexReg);
                                continue Loop;
                            case Icode_SETVAR1:
                                indexReg = iCode[frame.pc++];
                            // fallthrough
                            case Token.SETVAR:
                                stackTop =
                                        doSetVar(
                                                frame,
                                                stack,
                                                sDbl,
                                                stackTop,
                                                vars,
                                                varDbls,
                                                varAttributes,
                                                indexReg);
                                continue Loop;
                            case Icode_GETVAR1:
                                indexReg = iCode[frame.pc++];
                            // fallthrough
                            case Token.GETVAR:
                                stackTop =
                                        doGetVar(
                                                frame, stack, sDbl, stackTop, vars, varDbls,
                                                indexReg);
                                continue Loop;
                            case Icode_VAR_INC_DEC:
                                {
                                    stackTop =
                                            doVarIncDec(
                                                    cx,
                                                    frame,
                                                    stack,
                                                    sDbl,
                                                    stackTop,
                                                    vars,
                                                    varDbls,
                                                    varAttributes,
                                                    indexReg);
                                    continue Loop;
                                }
                            case Icode_ZERO:
                                ++stackTop;
                                stack[stackTop] = Integer.valueOf(0);
                                continue Loop;
                            case Icode_ONE:
                                ++stackTop;
                                stack[stackTop] = Integer.valueOf(1);
                                continue Loop;
                            case Token.NULL:
                                stack[++stackTop] = null;
                                continue Loop;
                            case Token.THIS:
                                stack[++stackTop] = frame.thisObj;
                                continue Loop;
                            case Token.SUPER:
                                {
                                    // See 9.1.1.3.5 GetSuperBase

                                    // If we are referring to "super", then we always have an
                                    // activation
                                    // (this is done in IrFactory). The home object is stored as
                                    // part of the
                                    // activation frame to propagate it correctly for nested
                                    // functions.
                                    Scriptable homeObject = getCurrentFrameHomeObject(frame);
                                    if (homeObject == null) {
                                        // This if is specified in the spec, but I cannot imagine
                                        // how the home object will ever be null since `super` is
                                        // legal _only_ in method definitions, where we do have a
                                        // home object!
                                        stack[++stackTop] = Undefined.instance;
                                    } else {
                                        stack[++stackTop] = homeObject.getPrototype();
                                    }
                                    continue Loop;
                                }
                            case Token.THISFN:
                                stack[++stackTop] = frame.fnOrScript;
                                continue Loop;
                            case Token.FALSE:
                                stack[++stackTop] = Boolean.FALSE;
                                continue Loop;
                            case Token.TRUE:
                                stack[++stackTop] = Boolean.TRUE;
                                continue Loop;
                            case Icode_UNDEF:
                                stack[++stackTop] = undefined;
                                continue Loop;
                            case Token.ENTERWITH:
                                {
                                    Object lhs = stack[stackTop];
                                    if (lhs == DOUBLE_MARK)
                                        lhs = ScriptRuntime.wrapNumber(sDbl[stackTop]);
                                    --stackTop;
                                    frame.scope = ScriptRuntime.enterWith(lhs, cx, frame.scope);
                                    continue Loop;
                                }
                            case Token.LEAVEWITH:
                                frame.scope = ScriptRuntime.leaveWith(frame.scope);
                                continue Loop;
                            case Token.CATCH_SCOPE:
                                {
                                    // stack top: exception object
                                    // stringReg: name of exception variable
                                    // indexReg: local for exception scope
                                    --stackTop;
                                    indexReg += iData.itsMaxVars;

                                    boolean afterFirstScope = (iData.itsICode[frame.pc] != 0);
                                    Throwable caughtException = (Throwable) stack[stackTop + 1];
                                    Scriptable lastCatchScope;
                                    if (!afterFirstScope) {
                                        lastCatchScope = null;
                                    } else {
                                        lastCatchScope = (Scriptable) stack[indexReg];
                                    }
                                    stack[indexReg] =
                                            ScriptRuntime.newCatchScope(
                                                    caughtException,
                                                    lastCatchScope,
                                                    stringReg,
                                                    cx,
                                                    frame.scope);
                                    ++frame.pc;
                                    continue Loop;
                                }
                            case Token.ENUM_INIT_KEYS:
                            case Token.ENUM_INIT_VALUES:
                            case Token.ENUM_INIT_ARRAY:
                            case Token.ENUM_INIT_VALUES_IN_ORDER:
                                {
                                    Object lhs = stack[stackTop];
                                    if (lhs == DOUBLE_MARK)
                                        lhs = ScriptRuntime.wrapNumber(sDbl[stackTop]);
                                    --stackTop;
                                    indexReg += iData.itsMaxVars;
                                    int enumType =
                                            op == Token.ENUM_INIT_KEYS
                                                    ? ScriptRuntime.ENUMERATE_KEYS
                                                    : op == Token.ENUM_INIT_VALUES
                                                            ? ScriptRuntime.ENUMERATE_VALUES
                                                            : op == Token.ENUM_INIT_VALUES_IN_ORDER
                                                                    ? ScriptRuntime
                                                                            .ENUMERATE_VALUES_IN_ORDER
                                                                    : ScriptRuntime.ENUMERATE_ARRAY;
                                    stack[indexReg] =
                                            ScriptRuntime.enumInit(lhs, cx, frame.scope, enumType);
                                    continue Loop;
                                }
                            case Token.ENUM_NEXT:
                            case Token.ENUM_ID:
                                {
                                    indexReg += iData.itsMaxVars;
                                    Object val = stack[indexReg];
                                    ++stackTop;
                                    stack[stackTop] =
                                            (op == Token.ENUM_NEXT)
                                                    ? ScriptRuntime.enumNext(val, cx)
                                                    : ScriptRuntime.enumId(val, cx);
                                    continue Loop;
                                }
                            case Token.REF_SPECIAL:
                                {
                                    // stringReg: name of special property
                                    Object obj = stack[stackTop];
                                    if (obj == DOUBLE_MARK)
                                        obj = ScriptRuntime.wrapNumber(sDbl[stackTop]);
                                    stack[stackTop] =
                                            ScriptRuntime.specialRef(
                                                    obj, stringReg, cx, frame.scope);
                                    continue Loop;
                                }
                            case Token.REF_MEMBER:
                                {
                                    // indexReg: flags
                                    stackTop = doRefMember(cx, stack, sDbl, stackTop, indexReg);
                                    continue Loop;
                                }
                            case Token.REF_NS_MEMBER:
                                {
                                    // indexReg: flags
                                    stackTop = doRefNsMember(cx, stack, sDbl, stackTop, indexReg);
                                    continue Loop;
                                }
                            case Token.REF_NAME:
                                {
                                    // indexReg: flags
                                    Object name = stack[stackTop];
                                    if (name == DOUBLE_MARK)
                                        name = ScriptRuntime.wrapNumber(sDbl[stackTop]);
                                    stack[stackTop] =
                                            ScriptRuntime.nameRef(name, cx, frame.scope, indexReg);
                                    continue Loop;
                                }
                            case Token.REF_NS_NAME:
                                {
                                    // indexReg: flags
                                    stackTop =
                                            doRefNsName(cx, frame, stack, sDbl, stackTop, indexReg);
                                    continue Loop;
                                }
                            case Icode_SCOPE_LOAD:
                                indexReg += iData.itsMaxVars;
                                frame.scope = (Scriptable) stack[indexReg];
                                continue Loop;
                            case Icode_SCOPE_SAVE:
                                indexReg += iData.itsMaxVars;
                                stack[indexReg] = frame.scope;
                                continue Loop;
                            case Icode_CLOSURE_EXPR:
                                InterpretedFunction fn =
                                        InterpretedFunction.createFunction(
                                                cx, frame.scope, frame.fnOrScript, indexReg);
                                if (fn.idata.itsFunctionType == FunctionNode.ARROW_FUNCTION) {
                                    Scriptable homeObject = getCurrentFrameHomeObject(frame);
                                    if (fn.idata.itsNeedsActivation) {
                                        fn.setHomeObject(homeObject);
                                    }

                                    stack[++stackTop] =
                                            new ArrowFunction(
                                                    cx, frame.scope, fn, frame.thisObj, homeObject);
                                } else {
                                    stack[++stackTop] = fn;
                                }
                                continue Loop;
                            case ICode_FN_STORE_HOME_OBJECT:
                                {
                                    // Stack contains: [object, keysArray, flagsArray, valuesArray,
                                    // function]
                                    InterpretedFunction fun = (InterpretedFunction) stack[stackTop];
                                    Scriptable homeObject = (Scriptable) stack[stackTop - 4];
                                    fun.setHomeObject(homeObject);
                                    continue Loop;
                                }
                            case Icode_CLOSURE_STMT:
                                initFunction(cx, frame.scope, frame.fnOrScript, indexReg);
                                continue Loop;
                            case Token.REGEXP:
                                Object re = iData.itsRegExpLiterals[indexReg];
                                stack[++stackTop] = ScriptRuntime.wrapRegExp(cx, frame.scope, re);
                                continue Loop;
                            case Icode_TEMPLATE_LITERAL_CALLSITE:
                                Object[] templateLiterals = iData.itsTemplateLiterals;
                                stack[++stackTop] =
                                        ScriptRuntime.getTemplateLiteralCallSite(
                                                cx, frame.scope, templateLiterals, indexReg);
                                continue Loop;
                            case Icode_LITERAL_NEW_OBJECT:
                                {
                                    // indexReg: index of constant with the keys
                                    Object[] ids = (Object[]) iData.literalIds[indexReg];
                                    boolean copyArray = iCode[frame.pc] != 0;
                                    ++frame.pc;
                                    ++stackTop;
                                    stack[stackTop] = cx.newObject(frame.scope);
                                    ++stackTop;
                                    stack[stackTop] =
                                            copyArray ? Arrays.copyOf(ids, ids.length) : ids;
                                    ++stackTop;
                                    stack[stackTop] = new int[ids.length];
                                    ++stackTop;
                                    stack[stackTop] = new Object[ids.length];
                                    sDbl[stackTop] = 0;
                                    continue Loop;
                                }
                            case Icode_LITERAL_NEW_ARRAY:
                                // indexReg: number of values in the literal
                                ++stackTop;
                                stack[stackTop] = new int[indexReg];
                                ++stackTop;
                                stack[stackTop] = new Object[indexReg];
                                sDbl[stackTop] = 0;
                                continue Loop;
                            case Icode_LITERAL_SET:
                                {
                                    Object value = stack[stackTop];
                                    if (value == DOUBLE_MARK)
                                        value = ScriptRuntime.wrapNumber(sDbl[stackTop]);
                                    --stackTop;
                                    int i = (int) sDbl[stackTop];
                                    ((Object[]) stack[stackTop])[i] = value;
                                    sDbl[stackTop] = i + 1;
                                    continue Loop;
                                }
                            case Icode_LITERAL_GETTER:
                                {
                                    Object value = stack[stackTop];
                                    --stackTop;
                                    int i = (int) sDbl[stackTop];
                                    ((Object[]) stack[stackTop])[i] = value;
                                    ((int[]) stack[stackTop - 1])[i] = -1;
                                    sDbl[stackTop] = i + 1;
                                    continue Loop;
                                }
                            case Icode_LITERAL_SETTER:
                                {
                                    Object value = stack[stackTop];
                                    --stackTop;
                                    int i = (int) sDbl[stackTop];
                                    ((Object[]) stack[stackTop])[i] = value;
                                    ((int[]) stack[stackTop - 1])[i] = 1;
                                    sDbl[stackTop] = i + 1;
                                    continue Loop;
                                }

                            case Icode_LITERAL_KEY_SET:
                                {
                                    Object key = stack[stackTop];
                                    if (key == DOUBLE_MARK)
                                        key = ScriptRuntime.wrapNumber(sDbl[stackTop]);
                                    --stackTop;
                                    Object[] ids = (Object[]) stack[stackTop - 2];
                                    int i = (int) sDbl[stackTop];
                                    ids[i] = key;
                                    continue Loop;
                                }
                            case Token.OBJECTLIT:
                                {
                                    Object[] values = (Object[]) stack[stackTop];
                                    --stackTop;
                                    int[] getterSetters = (int[]) stack[stackTop];
                                    --stackTop;
                                    Object[] keys = (Object[]) stack[stackTop];
                                    --stackTop;
                                    Scriptable object = (Scriptable) stack[stackTop];
                                    ScriptRuntime.fillObjectLiteral(
                                            object, keys, values, getterSetters, cx, frame.scope);
                                    continue Loop;
                                }
                            case Token.ARRAYLIT:
                            case Icode_SPARE_ARRAYLIT:
                                {
                                    Object[] data = (Object[]) stack[stackTop];
                                    --stackTop;
                                    int[] getterSetters = (int[]) stack[stackTop];
                                    Object val;

                                    int[] skipIndexces = null;
                                    if (op == Icode_SPARE_ARRAYLIT) {
                                        skipIndexces = (int[]) iData.literalIds[indexReg];
                                    }
                                    val =
                                            ScriptRuntime.newArrayLiteral(
                                                    data, skipIndexces, cx, frame.scope);

                                    stack[stackTop] = val;
                                    continue Loop;
                                }
                            case Icode_ENTERDQ:
                                {
                                    Object lhs = stack[stackTop];
                                    if (lhs == DOUBLE_MARK)
                                        lhs = ScriptRuntime.wrapNumber(sDbl[stackTop]);
                                    --stackTop;
                                    frame.scope = ScriptRuntime.enterDotQuery(lhs, frame.scope);
                                    continue Loop;
                                }
                            case Icode_LEAVEDQ:
                                {
                                    boolean valBln = stack_boolean(frame, stackTop);
                                    Object x = ScriptRuntime.updateDotQuery(valBln, frame.scope);
                                    if (x != null) {
                                        stack[stackTop] = x;
                                        frame.scope = ScriptRuntime.leaveDotQuery(frame.scope);
                                        frame.pc += 2;
                                        continue Loop;
                                    }
                                    // reset stack and PC to code after ENTERDQ
                                    --stackTop;
                                    break jumplessRun;
                                }
                            case Token.DEFAULTNAMESPACE:
                                {
                                    Object value = stack[stackTop];
                                    if (value == DOUBLE_MARK)
                                        value = ScriptRuntime.wrapNumber(sDbl[stackTop]);
                                    stack[stackTop] = ScriptRuntime.setDefaultNamespace(value, cx);
                                    continue Loop;
                                }
                            case Token.ESCXMLATTR:
                                {
                                    Object value = stack[stackTop];
                                    if (value != DOUBLE_MARK) {
                                        stack[stackTop] =
                                                ScriptRuntime.escapeAttributeValue(value, cx);
                                    }
                                    continue Loop;
                                }
                            case Token.ESCXMLTEXT:
                                {
                                    Object value = stack[stackTop];
                                    if (value != DOUBLE_MARK) {
                                        stack[stackTop] = ScriptRuntime.escapeTextValue(value, cx);
                                    }
                                    continue Loop;
                                }
                            case Icode_DEBUGGER:
                                if (frame.debuggerFrame != null) {
                                    frame.debuggerFrame.onDebuggerStatement(cx);
                                }
                                continue Loop;
                            case Icode_LINE:
                                frame.pcSourceLineStart = frame.pc;
                                if (frame.debuggerFrame != null) {
                                    int line = getIndex(iCode, frame.pc);
                                    frame.debuggerFrame.onLineChange(cx, line);
                                }
                                frame.pc += 2;
                                continue Loop;
                            case Icode_REG_IND_C0:
                                indexReg = 0;
                                continue Loop;
                            case Icode_REG_IND_C1:
                                indexReg = 1;
                                continue Loop;
                            case Icode_REG_IND_C2:
                                indexReg = 2;
                                continue Loop;
                            case Icode_REG_IND_C3:
                                indexReg = 3;
                                continue Loop;
                            case Icode_REG_IND_C4:
                                indexReg = 4;
                                continue Loop;
                            case Icode_REG_IND_C5:
                                indexReg = 5;
                                continue Loop;
                            case Icode_REG_IND1:
                                indexReg = 0xFF & iCode[frame.pc];
                                ++frame.pc;
                                continue Loop;
                            case Icode_REG_IND2:
                                indexReg = getIndex(iCode, frame.pc);
                                frame.pc += 2;
                                continue Loop;
                            case Icode_REG_IND4:
                                indexReg = getInt(iCode, frame.pc);
                                frame.pc += 4;
                                continue Loop;
                            default:
                                {
                                    NewState nextState;

                                    var insn = instructionObjs[-MIN_ICODE + op];
                                    if (insn == null) {
                                        dumpICode(iData);
                                        throw new RuntimeException(
                                                "Unknown icode : "
                                                        + op
                                                        + " @ pc : "
                                                        + (frame.pc - 1));
                                    }

                                    // The state will become
                                    // persistent for an entire call
                                    // once all instructions are moved
                                    // to this new scheme.
                                    InterpreterState state =
                                            new InterpreterState(
                                                    stackTop, indexReg, instructionCounting);
                                    state.stringReg = stringReg;
                                    state.bigIntReg = bigIntReg;
                                    state.throwable = throwable;
                                    state.generatorState = generatorState;

                                    nextState = insn.execute(cx, frame, state, op);

                                    stackTop = state.stackTop;
                                    indexReg = state.indexReg;
                                    stringReg = state.stringReg;
                                    bigIntReg = state.bigIntReg;
                                    throwable = state.throwable;
                                    generatorState = state.generatorState;

                                    if (nextState == null) {
                                        continue Loop;
                                    } else if (nextState == BREAK_LOOP) {
                                        break Loop;
                                    } else if (nextState == BREAK_JUMPLESSRUN) {
                                        break jumplessRun;
                                    } else if (nextState == BREAK_WITHOUT_EXTENSION) {
                                        break withoutExceptions;
                                    } else {
                                        return (InterpreterResult) nextState;
                                    }
                                }
                        } // end of interpreter switch
                    } // end of extra braces.
                } // end of jumplessRun label block

                // This should be reachable only for jump implementation
                // when pc points to encoded target offset
                if (instructionCounting) {
                    addInstructionCount(cx, frame, 2);
                }
                int offset = getShort(iCode, frame.pc);
                if (offset != 0) {
                    // -1 accounts for pc pointing to jump opcode + 1
                    frame.pc += offset - 1;
                } else {
                    frame.pc = iData.longJumps.get(frame.pc);
                }
                if (instructionCounting) {
                    frame.pcPrevBranch = frame.pc;
                }
                continue Loop;
            } // end of Loop: for

            exitFrame(cx, frame, null);
            if (frame.parentFrame != null) {
                CallFrame newFrame = frame.parentFrame;
                if (newFrame.frozen) {
                    newFrame = newFrame.cloneFrozen();
                }
                setCallResult(newFrame, frame.result, frame.resultDbl);
                return new StateContinueResult(newFrame, indexReg);
            }
            return new StateBreakResult(frame);

        } // end of interpreter withoutExceptions: try
        catch (Throwable ex) {
            if (throwable != null) {
                // This is serious bug and it is better to track it ASAP
                ex.printStackTrace(System.err);
                throw new IllegalStateException();
            }
            throwable = ex;
        }
        return new ThrowableResult(frame, throwable);
    }

    private static class DoStringCn extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            state.stringReg = frame.idata.itsStringTable[Icode_REG_STR_C0 - op];
            return null;
        }
    }

    private static class DoRegString1 extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            state.stringReg = frame.idata.itsStringTable[0xFF & frame.idata.itsICode[frame.pc]];
            ++frame.pc;
            return null;
        }
    }

    private static class DoRegString2 extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            state.stringReg = frame.idata.itsStringTable[getIndex(frame.idata.itsICode, frame.pc)];
            frame.pc += 2;
            return null;
        }
    }

    private static class DoRegString4 extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            state.stringReg = frame.idata.itsStringTable[getInt(frame.idata.itsICode, frame.pc)];
            frame.pc += 4;
            return null;
        }
    }

    private static class DoBigIntCn extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            state.bigIntReg = frame.idata.itsBigIntTable[Icode_REG_BIGINT_C0 - op];
            return null;
        }
    }

    private static class DoRegBigInt1 extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            state.bigIntReg = frame.idata.itsBigIntTable[0xFF & frame.idata.itsICode[frame.pc]];
            ++frame.pc;
            return null;
        }
    }

    private static class DoRegBigInt2 extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            state.bigIntReg = frame.idata.itsBigIntTable[getIndex(frame.idata.itsICode, frame.pc)];
            frame.pc += 2;
            return null;
        }
    }

    private static class DoRegBigInt4 extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            state.bigIntReg = frame.idata.itsBigIntTable[getInt(frame.idata.itsICode, frame.pc)];
            frame.pc += 4;
            return null;
        }
    }

    private static NewState doCallByteCode(
            Context cx,
            CallFrame frame,
            boolean instructionCounting,
            int op,
            int stackTop,
            int indexReg) {

        Object[] stack = frame.stack;
        double[] sDbl = frame.sDbl;
        Object[] boundArgs = null;
        int blen = 0;

        if (instructionCounting) {
            cx.instructionCount += INVOCATION_COST;
        }
        // stack change: lookup_result arg0 .. argN -> result
        // indexReg: number of arguments
        stackTop -= indexReg;
        ScriptRuntime.LookupResult result = (ScriptRuntime.LookupResult) stack[stackTop];
        // Check if the lookup result is a function and throw if it's not
        // must not be done sooner according to the spec
        Callable fun = result.getCallable();
        Scriptable funThisObj = result.getThis();
        Scriptable funHomeObj =
                (fun instanceof BaseFunction) ? ((BaseFunction) fun).getHomeObject() : null;
        if (op == Icode_CALL_ON_SUPER) {
            // funThisObj would have been the "super" object, which we
            // used to lookup the function. Now that that's done, we
            // discard it and invoke the function with the current
            // "this".
            funThisObj = frame.thisObj;
        }

        if (op == Token.REF_CALL) {
            Object[] outArgs = getArgsArray(stack, sDbl, stackTop + 1, indexReg);
            stack[stackTop] =
                    ScriptRuntime.callRef(
                            fun, funThisObj,
                            outArgs, cx);
            return new ContinueLoop(stackTop, indexReg);
        }
        Scriptable calleeScope = frame.scope;
        if (frame.useActivation) {
            calleeScope = ScriptableObject.getTopLevelScope(frame.scope);
        }
        // Iteratively reduce known function types: arrows, lambdas,
        // bound functions, call/apply, and no-such-method-handler in
        // order to make a best-effort to keep them in this interpreter
        // loop so continuations keep working. The loop initializer and
        // condition are formulated so that they short-circuit the loop
        // if the function is already an interpreted function, which
        // should be the majority of cases.
        for (; ; ) {
            if (fun instanceof ArrowFunction) {
                ArrowFunction afun = (ArrowFunction) fun;
                fun = afun.getTargetFunction();
                funThisObj = afun.getCallThis(cx);
                funHomeObj = afun.getBoundHomeObject();
            } else if (fun instanceof KnownBuiltInFunction) {
                KnownBuiltInFunction kfun = (KnownBuiltInFunction) fun;
                // Bug 405654 -- make the best effort to keep
                // Function.apply and Function.call within this
                // interpreter loop invocation
                if (BaseFunction.isApplyOrCall(kfun)) {
                    // funThisObj becomes fun
                    fun = ScriptRuntime.getCallable(funThisObj);
                    // first arg becomes thisObj
                    funThisObj = getApplyThis(cx, stack, sDbl, stackTop + 1, indexReg, fun, frame);
                    if (BaseFunction.isApply(kfun)) {
                        // Apply: second argument after new "this"
                        // should be array-like
                        // and we'll spread its elements on the stack
                        Object[] callArgs =
                                indexReg < 2
                                        ? ScriptRuntime.emptyArgs
                                        : ScriptRuntime.getApplyArguments(cx, stack[stackTop + 2]);
                        int alen = callArgs.length;
                        boundArgs = appendBoundArgs(boundArgs, callArgs);
                        blen = blen + alen;
                        indexReg = alen;
                    } else {
                        // Call: shift args left, starting from 2nd
                        if (indexReg > 0) {
                            if (indexReg > 1) {
                                System.arraycopy(
                                        stack, stackTop + 2, stack, stackTop + 1, indexReg - 1);
                                System.arraycopy(
                                        sDbl, stackTop + 2, sDbl, stackTop + 1, indexReg - 1);
                            }
                            indexReg--;
                        }
                    }
                } else {
                    // Some other IdFunctionObject we don't know how to
                    // reduce.
                    break;
                }
            } else if (fun instanceof LambdaConstructor) {
                break;
            } else if (fun instanceof LambdaFunction) {
                fun = ((LambdaFunction) fun).getTarget();
            } else if (fun instanceof BoundFunction) {
                BoundFunction bfun = (BoundFunction) fun;
                fun = bfun.getTargetFunction();
                funThisObj = bfun.getCallThis(cx, calleeScope);
                Object[] bArgs = bfun.getBoundArgs();
                boundArgs = appendBoundArgs(boundArgs, bArgs);
                blen = blen + bArgs.length;
                indexReg += blen;
            } else if (fun instanceof NoSuchMethodShim) {
                NoSuchMethodShim nsmfun = (NoSuchMethodShim) fun;
                // Bug 447697 -- make best effort to keep
                // __noSuchMethod__ within this interpreter loop
                // invocation.
                Object[] elements =
                        getArgsArray(stack, sDbl, boundArgs, blen, stackTop + 1, indexReg);
                fun = nsmfun.noSuchMethodMethod;
                boundArgs = new Object[2];
                blen = 2;
                boundArgs[0] = nsmfun.methodName;
                boundArgs[1] = cx.newArray(calleeScope, elements);
                indexReg = 2;
            } else if (fun == null) {
                throw ScriptRuntime.notFunctionError(null, null);
            } else {
                // Current function is something that we can't reduce
                // further.
                break;
            }
        }

        if (fun instanceof InterpretedFunction) {
            InterpretedFunction ifun = (InterpretedFunction) fun;
            if (frame.fnOrScript.securityDomain == ifun.securityDomain) {
                CallFrame callParentFrame = frame;
                if (op == Icode_TAIL_CALL) {
                    // In principle tail call can re-use the current
                    // frame and its stack arrays but it is hard to
                    // do properly. Any exceptions that can legally
                    // happen during frame re-initialization including
                    // StackOverflowException during innocent looking
                    // System.arraycopy may leave the current frame
                    // data corrupted leading to undefined behaviour
                    // in the catch code bellow that unwinds JS stack
                    // on exceptions. Then there is issue about frame
                    // release
                    // end exceptions there.
                    // To avoid frame allocation a released frame
                    // can be cached for re-use which would also benefit
                    // non-tail calls but it is not clear that this
                    // caching
                    // would gain in performance due to potentially
                    // bad interaction with GC.
                    callParentFrame = frame.parentFrame;
                    // Release the current frame. See Bug #344501 to see
                    // why
                    // it is being done here.
                    exitFrame(cx, frame, null);
                }
                CallFrame calleeFrame =
                        initFrame(
                                cx,
                                calleeScope,
                                funThisObj,
                                funHomeObj,
                                stack,
                                sDbl,
                                boundArgs,
                                stackTop + 1,
                                indexReg,
                                ifun,
                                callParentFrame);
                if (op != Icode_TAIL_CALL) {
                    frame.savedStackTop = stackTop;
                    frame.savedCallOp = op;
                }
                return new StateContinueResult(calleeFrame, indexReg);
            }
        }

        if (fun instanceof NativeContinuation) {
            // Jump to the captured continuation
            ContinuationJump cjump;
            cjump = new ContinuationJump((NativeContinuation) fun, frame);

            // continuation result is the first argument if any
            // of continuation call
            if (indexReg == 0) {
                cjump.result = undefined;
            } else {
                cjump.result = stack[stackTop + 1];
                cjump.resultDbl = sDbl[stackTop + 1];
            }

            // Start the real unwind job
            return new ThrowableResult(null, cjump);
        }

        if (fun instanceof IdFunctionObject) {
            IdFunctionObject ifun = (IdFunctionObject) fun;
            if (NativeContinuation.isContinuationConstructor(ifun)) {
                frame.stack[stackTop] = captureContinuation(cx, frame.parentFrame, false);
                return new ContinueLoop(stackTop, indexReg);
            }
        }

        frame.savedCallOp = op;
        frame.savedStackTop = stackTop;
        stack[stackTop] =
                fun.call(
                        cx,
                        calleeScope,
                        funThisObj,
                        getArgsArray(stack, sDbl, boundArgs, blen, stackTop + 1, indexReg));

        return new ContinueLoop(stackTop, indexReg);
    }

    private static Object[] appendBoundArgs(Object[] boundArgs, Object[] newArgs) {
        if (newArgs.length == 0) {
            return boundArgs;
        } else if (boundArgs == null) {
            return newArgs;
        } else {
            Object[] result = Arrays.copyOf(boundArgs, boundArgs.length + newArgs.length);
            System.arraycopy(newArgs, 0, result, boundArgs.length, newArgs.length);
            return result;
        }
    }

    private static Scriptable getCurrentFrameHomeObject(CallFrame frame) {
        if (frame.scope instanceof NativeCall) {
            return ((NativeCall) frame.scope).getHomeObject();
        } else {
            return null;
        }
    }

    private static int doInOrInstanceof(
            Context cx, int op, Object[] stack, double[] sDbl, int stackTop) {
        Object rhs = stack[stackTop];
        if (rhs == DOUBLE_MARK) rhs = ScriptRuntime.wrapNumber(sDbl[stackTop]);
        --stackTop;
        Object lhs = stack[stackTop];
        if (lhs == DOUBLE_MARK) lhs = ScriptRuntime.wrapNumber(sDbl[stackTop]);
        boolean valBln;
        if (op == Token.IN) {
            valBln = ScriptRuntime.in(lhs, rhs, cx);
        } else {
            valBln = ScriptRuntime.instanceOf(lhs, rhs, cx);
        }
        stack[stackTop] = ScriptRuntime.wrapBoolean(valBln);
        return stackTop;
    }

    private static int doCompare(
            CallFrame frame, int op, Object[] stack, double[] sDbl, int stackTop) {
        --stackTop;
        Object rhs = stack[stackTop + 1];
        Object lhs = stack[stackTop];
        boolean valBln;
        if (lhs == DOUBLE_MARK && rhs == DOUBLE_MARK) {
            valBln = ScriptRuntime.compareTo(sDbl[stackTop], sDbl[stackTop + 1], op);
            stack[stackTop] = valBln;
            return stackTop;
        }
        object_compare:
        {
            number_compare:
            {
                Number rNum, lNum;
                if (rhs == DOUBLE_MARK) {
                    rNum = sDbl[stackTop + 1];
                    lNum = stack_numeric(frame, stackTop);
                } else if (lhs == DOUBLE_MARK) {
                    rNum = ScriptRuntime.toNumeric(rhs);
                    lNum = sDbl[stackTop];
                } else {
                    break number_compare;
                }
                valBln = ScriptRuntime.compare(lNum, rNum, op);
                break object_compare;
            }
            valBln = ScriptRuntime.compare(lhs, rhs, op);
        }
        stack[stackTop] = ScriptRuntime.wrapBoolean(valBln);
        return stackTop;
    }

    private static int doFastBitOp(
            CallFrame frame, int op, Object[] stack, double[] sDbl, int stackTop) {
        double lValue = sDbl[stackTop - 1];
        double rValue = sDbl[stackTop];
        stackTop--;

        double result = 0.0;
        switch (op) {
            case Token.BITAND:
                result = ScriptRuntime.bitwiseAND(lValue, rValue);
                break;
            case Token.BITOR:
                result = ScriptRuntime.bitwiseOR(lValue, rValue);
                break;
            case Token.BITXOR:
                result = ScriptRuntime.bitwiseXOR(lValue, rValue);
                break;
            case Token.LSH:
                result = ScriptRuntime.leftShift(lValue, rValue);
                break;
            case Token.RSH:
                result = ScriptRuntime.signedRightShift(lValue, rValue);
                break;
        }

        stack[stackTop] = DOUBLE_MARK;
        sDbl[stackTop] = result;

        return stackTop;
    }

    private static int doBitOp(
            CallFrame frame, int op, Object[] stack, double[] sDbl, int stackTop) {
        if (stack[stackTop] == DOUBLE_MARK && stack[stackTop - 1] == DOUBLE_MARK) {
            return doFastBitOp(frame, op, stack, sDbl, stackTop);
        }
        Number lValue = stack_numeric(frame, stackTop - 1);
        Number rValue = stack_numeric(frame, stackTop);
        stackTop--;

        Number result = null;
        switch (op) {
            case Token.BITAND:
                result = ScriptRuntime.bitwiseAND(lValue, rValue);
                break;
            case Token.BITOR:
                result = ScriptRuntime.bitwiseOR(lValue, rValue);
                break;
            case Token.BITXOR:
                result = ScriptRuntime.bitwiseXOR(lValue, rValue);
                break;
            case Token.LSH:
                result = ScriptRuntime.leftShift(lValue, rValue);
                break;
            case Token.RSH:
                result = ScriptRuntime.signedRightShift(lValue, rValue);
                break;
        }

        if (result instanceof BigInteger) {
            stack[stackTop] = result;
        } else {
            stack[stackTop] = DOUBLE_MARK;
            sDbl[stackTop] = result.doubleValue();
        }
        return stackTop;
    }

    private static int doBitNOT(CallFrame frame, Object[] stack, double[] sDbl, int stackTop) {
        Number value = stack_numeric(frame, stackTop);
        Number result = ScriptRuntime.bitwiseNOT(value);
        if (result instanceof BigInteger) {
            stack[stackTop] = result;
        } else {
            stack[stackTop] = DOUBLE_MARK;
            sDbl[stackTop] = result.doubleValue();
        }
        return stackTop;
    }

    private static int doDelName(
            Context cx, CallFrame frame, int op, Object[] stack, double[] sDbl, int stackTop) {
        Object rhs = stack[stackTop];
        if (rhs == DOUBLE_MARK) rhs = ScriptRuntime.wrapNumber(sDbl[stackTop]);
        --stackTop;
        Object lhs = stack[stackTop];
        if (lhs == DOUBLE_MARK) lhs = ScriptRuntime.wrapNumber(sDbl[stackTop]);
        stack[stackTop] = ScriptRuntime.delete(lhs, rhs, cx, frame.scope, op == Icode_DELNAME);
        return stackTop;
    }

    private static int doGetElem(
            Context cx, CallFrame frame, Object[] stack, double[] sDbl, int stackTop) {
        --stackTop;
        Object lhs = stack[stackTop];
        if (lhs == DOUBLE_MARK) {
            lhs = ScriptRuntime.wrapNumber(sDbl[stackTop]);
        }
        Object value;
        Object id = stack[stackTop + 1];
        if (id != DOUBLE_MARK) {
            value = ScriptRuntime.getObjectElem(lhs, id, cx, frame.scope);
        } else {
            double d = sDbl[stackTop + 1];
            value = ScriptRuntime.getObjectIndex(lhs, d, cx, frame.scope);
        }
        stack[stackTop] = value;
        return stackTop;
    }

    private static int doGetElemSuper(
            Context cx, CallFrame frame, Object[] stack, double[] sDbl, int stackTop) {
        --stackTop;
        Object superObject = stack[stackTop];
        if (superObject == DOUBLE_MARK) Kit.codeBug();
        Object value;
        Object id = stack[stackTop + 1];
        if (id != DOUBLE_MARK) {
            value = ScriptRuntime.getSuperElem(superObject, id, cx, frame.scope, frame.thisObj);
        } else {
            double d = sDbl[stackTop + 1];
            value = ScriptRuntime.getSuperIndex(superObject, d, cx, frame.scope, frame.thisObj);
        }
        stack[stackTop] = value;
        return stackTop;
    }

    private static int doSetElem(
            Context cx, CallFrame frame, Object[] stack, double[] sDbl, int stackTop) {
        stackTop -= 2;
        Object rhs = stack[stackTop + 2];
        if (rhs == DOUBLE_MARK) {
            rhs = ScriptRuntime.wrapNumber(sDbl[stackTop + 2]);
        }
        Object lhs = stack[stackTop];
        if (lhs == DOUBLE_MARK) {
            lhs = ScriptRuntime.wrapNumber(sDbl[stackTop]);
        }
        Object value;
        Object id = stack[stackTop + 1];
        if (id != DOUBLE_MARK) {
            value = ScriptRuntime.setObjectElem(lhs, id, rhs, cx, frame.scope);
        } else {
            double d = sDbl[stackTop + 1];
            value = ScriptRuntime.setObjectIndex(lhs, d, rhs, cx, frame.scope);
        }
        stack[stackTop] = value;
        return stackTop;
    }

    private static int doSetElemSuper(
            Context cx, CallFrame frame, Object[] stack, double[] sDbl, int stackTop) {
        stackTop -= 2;
        Object rhs = stack[stackTop + 2];
        if (rhs == DOUBLE_MARK) {
            rhs = ScriptRuntime.wrapNumber(sDbl[stackTop + 2]);
        }
        Object superObject = stack[stackTop];
        if (superObject == DOUBLE_MARK) Kit.codeBug();
        Object value;
        Object id = stack[stackTop + 1];
        if (id != DOUBLE_MARK) {
            value =
                    ScriptRuntime.setSuperElem(
                            superObject, id, rhs, cx, frame.scope, frame.thisObj);
        } else {
            double d = sDbl[stackTop + 1];
            value =
                    ScriptRuntime.setSuperIndex(
                            superObject, d, rhs, cx, frame.scope, frame.thisObj);
        }
        stack[stackTop] = value;
        return stackTop;
    }

    private static int doElemIncDec(
            Context cx,
            CallFrame frame,
            byte[] iCode,
            Object[] stack,
            double[] sDbl,
            int stackTop) {
        Object rhs = stack[stackTop];
        if (rhs == DOUBLE_MARK) rhs = ScriptRuntime.wrapNumber(sDbl[stackTop]);
        --stackTop;
        Object lhs = stack[stackTop];
        if (lhs == DOUBLE_MARK) lhs = ScriptRuntime.wrapNumber(sDbl[stackTop]);
        stack[stackTop] = ScriptRuntime.elemIncrDecr(lhs, rhs, cx, frame.scope, iCode[frame.pc]);
        ++frame.pc;
        return stackTop;
    }

    private static int doCallSpecial(
            Context cx,
            CallFrame frame,
            Object[] stack,
            double[] sDbl,
            int stackTop,
            byte[] iCode,
            int indexReg,
            boolean isOptionalChainingCall) {
        int callType = iCode[frame.pc] & 0xFF;
        boolean isNew = (iCode[frame.pc + 1] != 0);
        int sourceLine = getIndex(iCode, frame.pc + 2);

        // indexReg: number of arguments
        if (isNew) {
            // stack change: function arg0 .. argN -> newResult
            stackTop -= indexReg;

            Object function = stack[stackTop];
            if (function == DOUBLE_MARK) function = ScriptRuntime.wrapNumber(sDbl[stackTop]);
            Object[] outArgs = getArgsArray(stack, sDbl, stackTop + 1, indexReg);
            stack[stackTop] =
                    ScriptRuntime.newSpecial(cx, function, outArgs, frame.scope, callType);
        } else {
            // stack change: function thisObj arg0 .. argN -> result
            stackTop -= indexReg;

            // Call code generation ensure that stack here
            // is ... Callable Scriptable
            ScriptRuntime.LookupResult result = (ScriptRuntime.LookupResult) stack[stackTop];
            Object[] outArgs = getArgsArray(stack, sDbl, stackTop + 1, indexReg);
            Callable function = result.getCallable();
            stack[stackTop] =
                    ScriptRuntime.callSpecial(
                            cx,
                            function,
                            result.getThis(),
                            outArgs,
                            frame.scope,
                            frame.thisObj,
                            callType,
                            frame.idata.itsSourceFile,
                            sourceLine,
                            isOptionalChainingCall);
        }
        frame.pc += 4;
        return stackTop;
    }

    private static int doSetConstVar(
            CallFrame frame,
            Object[] stack,
            double[] sDbl,
            int stackTop,
            Object[] vars,
            double[] varDbls,
            byte[] varAttributes,
            int indexReg) {
        if (!frame.useActivation) {
            if ((varAttributes[indexReg] & ScriptableObject.READONLY) == 0) {
                throw Context.reportRuntimeErrorById(
                        "msg.var.redecl", frame.idata.argNames[indexReg]);
            }
            if ((varAttributes[indexReg] & ScriptableObject.UNINITIALIZED_CONST) != 0) {
                vars[indexReg] = stack[stackTop];
                varAttributes[indexReg] &= ~ScriptableObject.UNINITIALIZED_CONST;
                varDbls[indexReg] = sDbl[stackTop];
            }
        } else {
            Object val = stack[stackTop];
            if (val == DOUBLE_MARK) val = ScriptRuntime.wrapNumber(sDbl[stackTop]);
            String stringReg = frame.idata.argNames[indexReg];
            if (frame.scope instanceof ConstProperties) {
                ConstProperties cp = (ConstProperties) frame.scope;
                cp.putConst(stringReg, frame.scope, val);
            } else throw Kit.codeBug();
        }
        return stackTop;
    }

    private static int doSetVar(
            CallFrame frame,
            Object[] stack,
            double[] sDbl,
            int stackTop,
            Object[] vars,
            double[] varDbls,
            byte[] varAttributes,
            int indexReg) {
        if (!frame.useActivation) {
            if ((varAttributes[indexReg] & ScriptableObject.READONLY) == 0) {
                vars[indexReg] = stack[stackTop];
                varDbls[indexReg] = sDbl[stackTop];
            }
        } else {
            Object val = stack[stackTop];
            if (val == DOUBLE_MARK) val = ScriptRuntime.wrapNumber(sDbl[stackTop]);
            String stringReg = frame.idata.argNames[indexReg];
            frame.scope.put(stringReg, frame.scope, val);
        }
        return stackTop;
    }

    private static int doGetVar(
            CallFrame frame,
            Object[] stack,
            double[] sDbl,
            int stackTop,
            Object[] vars,
            double[] varDbls,
            int indexReg) {
        ++stackTop;
        if (!frame.useActivation) {
            stack[stackTop] = vars[indexReg];
            sDbl[stackTop] = varDbls[indexReg];
        } else {
            String stringReg = frame.idata.argNames[indexReg];
            stack[stackTop] = frame.scope.get(stringReg, frame.scope);
        }
        return stackTop;
    }

    private static int doVarIncDec(
            Context cx,
            CallFrame frame,
            Object[] stack,
            double[] sDbl,
            int stackTop,
            Object[] vars,
            double[] varDbls,
            byte[] varAttributes,
            int indexReg) {
        // indexReg : varindex
        ++stackTop;
        int incrDecrMask = frame.idata.itsICode[frame.pc];
        if (!frame.useActivation) {
            Object varValue = vars[indexReg];
            double d = 0.0;
            BigInteger bi = null;
            if (varValue == DOUBLE_MARK) {
                d = varDbls[indexReg];
            } else {
                Number num = ScriptRuntime.toNumeric(varValue);
                if (num instanceof BigInteger) {
                    bi = (BigInteger) num;
                } else {
                    d = num.doubleValue();
                }
            }
            if (bi == null) {
                // double
                double d2 = ((incrDecrMask & Node.DECR_FLAG) == 0) ? d + 1.0 : d - 1.0;
                boolean post = ((incrDecrMask & Node.POST_FLAG) != 0);
                if ((varAttributes[indexReg] & ScriptableObject.READONLY) == 0) {
                    if (varValue != DOUBLE_MARK) {
                        vars[indexReg] = DOUBLE_MARK;
                    }
                    varDbls[indexReg] = d2;
                    stack[stackTop] = DOUBLE_MARK;
                    sDbl[stackTop] = post ? d : d2;
                } else {
                    if (post && varValue != DOUBLE_MARK) {
                        stack[stackTop] = varValue;
                    } else {
                        stack[stackTop] = DOUBLE_MARK;
                        sDbl[stackTop] = post ? d : d2;
                    }
                }
            } else {
                // BigInt
                BigInteger result;
                if ((incrDecrMask & Node.DECR_FLAG) == 0) {
                    result = bi.add(BigInteger.ONE);
                } else {
                    result = bi.subtract(BigInteger.ONE);
                }

                boolean post = ((incrDecrMask & Node.POST_FLAG) != 0);
                if ((varAttributes[indexReg] & ScriptableObject.READONLY) == 0) {
                    vars[indexReg] = result;
                    stack[stackTop] = post ? bi : result;
                } else {
                    if (post && varValue != DOUBLE_MARK) {
                        stack[stackTop] = varValue;
                    } else {
                        stack[stackTop] = post ? bi : result;
                    }
                }
            }
        } else {
            String varName = frame.idata.argNames[indexReg];
            stack[stackTop] = ScriptRuntime.nameIncrDecr(frame.scope, varName, cx, incrDecrMask);
        }
        ++frame.pc;
        return stackTop;
    }

    private static int doRefMember(
            Context cx, Object[] stack, double[] sDbl, int stackTop, int flags) {
        Object elem = stack[stackTop];
        if (elem == DOUBLE_MARK) elem = ScriptRuntime.wrapNumber(sDbl[stackTop]);
        --stackTop;
        Object obj = stack[stackTop];
        if (obj == DOUBLE_MARK) obj = ScriptRuntime.wrapNumber(sDbl[stackTop]);
        stack[stackTop] = ScriptRuntime.memberRef(obj, elem, cx, flags);
        return stackTop;
    }

    private static int doRefNsMember(
            Context cx, Object[] stack, double[] sDbl, int stackTop, int flags) {
        Object elem = stack[stackTop];
        if (elem == DOUBLE_MARK) elem = ScriptRuntime.wrapNumber(sDbl[stackTop]);
        --stackTop;
        Object ns = stack[stackTop];
        if (ns == DOUBLE_MARK) ns = ScriptRuntime.wrapNumber(sDbl[stackTop]);
        --stackTop;
        Object obj = stack[stackTop];
        if (obj == DOUBLE_MARK) obj = ScriptRuntime.wrapNumber(sDbl[stackTop]);
        stack[stackTop] = ScriptRuntime.memberRef(obj, ns, elem, cx, flags);
        return stackTop;
    }

    private static int doRefNsName(
            Context cx, CallFrame frame, Object[] stack, double[] sDbl, int stackTop, int flags) {
        Object name = stack[stackTop];
        if (name == DOUBLE_MARK) name = ScriptRuntime.wrapNumber(sDbl[stackTop]);
        --stackTop;
        Object ns = stack[stackTop];
        if (ns == DOUBLE_MARK) ns = ScriptRuntime.wrapNumber(sDbl[stackTop]);
        stack[stackTop] = ScriptRuntime.nameRef(ns, name, cx, frame.scope, flags);
        return stackTop;
    }

    private static boolean doEquals(Object[] stack, double[] sDbl, int stackTop) {
        Object rhs = stack[stackTop + 1];
        Object lhs = stack[stackTop];
        if (rhs == DOUBLE_MARK) {
            if (lhs == DOUBLE_MARK) {
                return (sDbl[stackTop] == sDbl[stackTop + 1]);
            }
            return ScriptRuntime.eqNumber(sDbl[stackTop + 1], lhs);
        }
        if (lhs == DOUBLE_MARK) {
            return ScriptRuntime.eqNumber(sDbl[stackTop], rhs);
        }
        return ScriptRuntime.eq(lhs, rhs);
    }

    private static boolean doShallowEquals(Object[] stack, double[] sDbl, int stackTop) {
        Object rhs = stack[stackTop + 1];
        Object lhs = stack[stackTop];
        double rdbl, ldbl;
        if (rhs == DOUBLE_MARK) {
            rdbl = sDbl[stackTop + 1];
            if (lhs == DOUBLE_MARK) {
                ldbl = sDbl[stackTop];
            } else if (lhs instanceof Number && !(lhs instanceof BigInteger)) {
                ldbl = ((Number) lhs).doubleValue();
            } else {
                return false;
            }
        } else if (lhs == DOUBLE_MARK) {
            ldbl = sDbl[stackTop];
            if (rhs instanceof Number && !(rhs instanceof BigInteger)) {
                rdbl = ((Number) rhs).doubleValue();
            } else {
                return false;
            }
        } else {
            return ScriptRuntime.shallowEq(lhs, rhs);
        }
        return (ldbl == rdbl);
    }

    private static CallFrame processThrowable(
            Context cx,
            Object throwable,
            CallFrame frame,
            int indexReg,
            boolean instructionCounting) {
        // Recovering from exception, indexReg contains
        // the index of handler

        if (indexReg >= 0) {
            // Normal exception handler, transfer
            // control appropriately

            if (frame.frozen) {
                // XXX Deal with exceptios!!!
                frame = frame.cloneFrozen();
            }

            int[] table = frame.idata.itsExceptionTable;

            frame.pc = table[indexReg + EXCEPTION_HANDLER_SLOT];
            if (instructionCounting) {
                frame.pcPrevBranch = frame.pc;
            }

            frame.savedStackTop = frame.emptyStackTop;
            int localShift = frame.idata.itsMaxVars;
            int scopeLocal = localShift + table[indexReg + EXCEPTION_SCOPE_SLOT];
            int exLocal = localShift + table[indexReg + EXCEPTION_LOCAL_SLOT];
            frame.scope = (Scriptable) frame.stack[scopeLocal];
            frame.stack[exLocal] = throwable;

            throwable = null;
        } else {
            // Continuation restoration
            ContinuationJump cjump = (ContinuationJump) throwable;

            // Clear throwable to indicate that exceptions are OK
            throwable = null;

            if (!Objects.equals(cjump.branchFrame, frame)) Kit.codeBug();

            // Check that we have at least one frozen frame
            // in the case of detached continuation restoration:
            // unwind code ensure that
            if (cjump.capturedFrame == null) Kit.codeBug();

            // Need to rewind branchFrame, capturedFrame
            // and all frames in between
            int rewindCount = cjump.capturedFrame.frameIndex + 1;
            if (cjump.branchFrame != null) {
                rewindCount -= cjump.branchFrame.frameIndex;
            }

            int enterCount = 0;
            CallFrame[] enterFrames = null;

            CallFrame x = cjump.capturedFrame;
            for (int i = 0; i != rewindCount; ++i) {
                if (!x.frozen) Kit.codeBug();
                if (x.useActivation) {
                    if (enterFrames == null) {
                        // Allocate enough space to store the rest
                        // of rewind frames in case all of them
                        // would require to enter
                        enterFrames = new CallFrame[rewindCount - i];
                    }
                    enterFrames[enterCount] = x;
                    ++enterCount;
                }
                x = x.parentFrame;
            }

            while (enterCount != 0) {
                // execute enter: walk enterFrames in the reverse
                // order since they were stored starting from
                // the capturedFrame, not branchFrame
                --enterCount;
                x = enterFrames[enterCount];
                enterFrame(cx, x, ScriptRuntime.emptyArgs, true);
            }

            // Continuation jump is almost done: capturedFrame
            // points to the call to the function that captured
            // continuation, so clone capturedFrame and
            // emulate return that function with the suplied result
            frame = cjump.capturedFrame.cloneFrozen();
            setCallResult(frame, cjump.result, cjump.resultDbl);
            // restart the execution
        }
        frame.throwable = throwable;
        return frame;
    }

    private static Object freezeGenerator(
            Context cx,
            CallFrame frame,
            int stackTop,
            GeneratorState generatorState,
            boolean yieldStar) {
        if (generatorState.operation == NativeGenerator.GENERATOR_CLOSE) {
            // Error: no yields when generator is closing
            throw ScriptRuntime.typeErrorById("msg.yield.closing");
        }
        // return to our caller (which should be a method of NativeGenerator)
        frame.frozen = true;
        frame.result = frame.stack[stackTop];
        frame.resultDbl = frame.sDbl[stackTop];
        frame.savedStackTop = stackTop;
        frame.pc--; // we want to come back here when we resume
        ScriptRuntime.exitActivationFunction(cx);
        final Object result =
                (frame.result != DOUBLE_MARK)
                        ? frame.result
                        : ScriptRuntime.wrapNumber(frame.resultDbl);
        if (yieldStar) {
            return new ES6Generator.YieldStarResult(result);
        }
        return result;
    }

    private static Object thawGenerator(
            CallFrame frame, int stackTop, GeneratorState generatorState, int op) {
        // we are resuming execution
        frame.frozen = false;
        int sourceLine = getIndex(frame.idata.itsICode, frame.pc);
        frame.pc += 2; // skip line number data
        if (generatorState.operation == NativeGenerator.GENERATOR_THROW) {
            // processing a call to <generator>.throw(exception): must
            // act as if exception was thrown from resumption point.
            return new JavaScriptException(
                    generatorState.value, frame.idata.itsSourceFile, sourceLine);
        }
        if (generatorState.operation == NativeGenerator.GENERATOR_CLOSE) {
            return generatorState.value;
        }
        if (generatorState.operation != NativeGenerator.GENERATOR_SEND) throw Kit.codeBug();
        if ((op == Token.YIELD) || (op == Icode_YIELD_STAR)) {
            frame.stack[stackTop] = generatorState.value;
        }
        return Scriptable.NOT_FOUND;
    }

    private static Scriptable getApplyThis(
            Context cx,
            Object[] stack,
            double[] sDbl,
            int thisIdx,
            int indexReg,
            Callable target,
            CallFrame frame) {
        Object obj;
        if (indexReg != 0) {
            obj = stack[thisIdx];
            if (obj == DOUBLE_MARK) obj = ScriptRuntime.wrapNumber(sDbl[thisIdx]);
        } else {
            obj = null;
        }
        return ScriptRuntime.getApplyOrCallThis(cx, frame.scope, obj, indexReg, target);
    }

    private static CallFrame initFrame(
            Context cx,
            Scriptable callerScope,
            Scriptable thisObj,
            Scriptable homeObj,
            Object[] args,
            double[] argsDbl,
            Object[] boundArgs,
            int argShift,
            int argCount,
            InterpretedFunction fnOrScript,
            CallFrame parentFrame) {
        CallFrame frame =
                new CallFrame(
                        cx,
                        thisObj,
                        fnOrScript,
                        parentFrame,
                        parentFrame == null
                                ? (CallFrame) cx.lastInterpreterFrame
                                : parentFrame.previousInterpreterFrame);
        frame.initializeArgs(
                cx, callerScope, args, argsDbl, boundArgs, argShift, argCount, homeObj);
        enterFrame(cx, frame, args, false);
        return frame;
    }

    private static void enterFrame(
            Context cx, CallFrame frame, Object[] args, boolean continuationRestart) {
        boolean usesActivation = frame.idata.itsNeedsActivation;
        boolean isDebugged = frame.debuggerFrame != null;
        if (usesActivation || isDebugged) {
            Scriptable scope = frame.scope;
            if (scope == null) {
                Kit.codeBug();
            } else if (continuationRestart) {
                // Walk the parent chain of frame.scope until a NativeCall is
                // found. Normally, frame.scope is a NativeCall when called
                // from initFrame() for a debugged or activatable function.
                // However, when called from interpretLoop() as part of
                // restarting a continuation, it can also be a NativeWith if
                // the continuation was captured within a "with" or "catch"
                // block ("catch" implicitly uses NativeWith to create a scope
                // to expose the exception variable).
                for (; ; ) {
                    if (scope instanceof NativeWith) {
                        scope = scope.getParentScope();
                        if (scope == null
                                || (frame.parentFrame != null
                                        && frame.parentFrame.scope == scope)) {
                            // If we get here, we didn't find a NativeCall in
                            // the call chain before reaching parent frame's
                            // scope. This should not be possible.
                            Kit.codeBug();
                            break; // Never reached, but keeps the static analyzer
                            // happy about "scope" not being null 5 lines above.
                        }
                    } else {
                        break;
                    }
                }
            }
            if (isDebugged) {
                frame.debuggerFrame.onEnter(cx, scope, frame.thisObj, args);
            }
            // Enter activation only when itsNeedsActivation true,
            // since debugger should not interfere with activation
            // chaining
            if (usesActivation) {
                ScriptRuntime.enterActivationFunction(cx, scope);
            }
        }
    }

    private static void exitFrame(Context cx, CallFrame frame, Object throwable) {
        if (frame.idata.itsNeedsActivation) {
            ScriptRuntime.exitActivationFunction(cx);
        }

        if (frame.debuggerFrame != null) {
            try {
                if (throwable instanceof Throwable) {
                    frame.debuggerFrame.onExit(cx, true, throwable);
                } else {
                    Object result;
                    ContinuationJump cjump = (ContinuationJump) throwable;
                    if (cjump == null) {
                        result = frame.result;
                    } else {
                        result = cjump.result;
                    }
                    if (result == DOUBLE_MARK) {
                        double resultDbl;
                        if (cjump == null) {
                            resultDbl = frame.resultDbl;
                        } else {
                            resultDbl = cjump.resultDbl;
                        }
                        result = ScriptRuntime.wrapNumber(resultDbl);
                    }
                    frame.debuggerFrame.onExit(cx, false, result);
                }
            } catch (Throwable ex) {
                System.err.println("RHINO USAGE WARNING: onExit terminated with exception");
                ex.printStackTrace(System.err);
            }
        }
    }

    private static void setCallResult(CallFrame frame, Object callResult, double callResultDbl) {
        if (frame.savedCallOp == Token.CALL || frame.savedCallOp == Icode_CALL_ON_SUPER) {
            frame.stack[frame.savedStackTop] = callResult;
            frame.sDbl[frame.savedStackTop] = callResultDbl;
        } else if (frame.savedCallOp == Token.NEW) {
            // If construct returns scriptable,
            // then it replaces on stack top saved original instance
            // of the object.
            if (callResult instanceof Scriptable) {
                frame.stack[frame.savedStackTop] = callResult;
            }
        } else {
            Kit.codeBug();
        }
        frame.savedCallOp = 0;
    }

    public static NativeContinuation captureContinuation(Context cx) {
        if (cx.lastInterpreterFrame == null || !(cx.lastInterpreterFrame instanceof CallFrame)) {
            throw new IllegalStateException("Interpreter frames not found");
        }
        return captureContinuation(cx, (CallFrame) cx.lastInterpreterFrame, true);
    }

    private static NativeContinuation captureContinuation(
            Context cx, CallFrame frame, boolean requireContinuationsTopFrame) {
        NativeContinuation c = new NativeContinuation();
        ScriptRuntime.setObjectProtoAndParent(c, ScriptRuntime.getTopCallScope(cx));

        // Make sure that all frames are frozen
        CallFrame x = frame;
        CallFrame outermost = frame;
        while (x != null && !x.frozen) {
            x.frozen = true;
            // Allow to GC unused stack space
            for (int i = x.savedStackTop + 1; i != x.stack.length; ++i) {
                // Allow to GC unused stack space
                x.stack[i] = null;
                x.stackAttributes[i] = ScriptableObject.EMPTY;
            }
            if (x.savedCallOp == Token.CALL || x.savedCallOp == Icode_CALL_ON_SUPER) {
                // the call will always overwrite the stack top with the result
                x.stack[x.savedStackTop] = null;
            } else {
                if (x.savedCallOp != Token.NEW) Kit.codeBug();
                // the new operator uses stack top to store the constructed
                // object so it shall not be cleared: see comments in
                // setCallResult
            }
            outermost = x;
            x = x.parentFrame;
        }

        if (requireContinuationsTopFrame) {
            while (outermost.parentFrame != null) outermost = outermost.parentFrame;

            if (!outermost.isContinuationsTopFrame) {
                throw new IllegalStateException(
                        "Cannot capture continuation "
                                + "from JavaScript code not called directly by "
                                + "executeScriptWithContinuations or "
                                + "callFunctionWithContinuations");
            }
        }

        c.initImplementation(frame);
        return c;
    }

    private static int stack_int32(CallFrame frame, int i) {
        Object x = frame.stack[i];
        if (x == UniqueTag.DOUBLE_MARK) {
            return ScriptRuntime.toInt32(frame.sDbl[i]);
        }
        return ScriptRuntime.toInt32(x);
    }

    private static double stack_double(CallFrame frame, int i) {
        Object x = frame.stack[i];
        if (x != UniqueTag.DOUBLE_MARK) {
            return ScriptRuntime.toNumber(x);
        }
        return frame.sDbl[i];
    }

    private static Number stack_numeric(CallFrame frame, int i) {
        Object x = frame.stack[i];
        if (x != UniqueTag.DOUBLE_MARK) {
            return ScriptRuntime.toNumeric(x);
        }
        return frame.sDbl[i];
    }

    private static boolean stack_boolean(CallFrame frame, int i) {
        Object x = frame.stack[i];
        if (Boolean.TRUE.equals(x)) {
            return true;
        } else if (Boolean.FALSE.equals(x)) {
            return false;
        } else if (x == UniqueTag.DOUBLE_MARK) {
            double d = frame.sDbl[i];
            return !Double.isNaN(d) && d != 0.0;
        } else if (x == null || x == Undefined.instance) {
            return false;
        } else if (x instanceof BigInteger) {
            return !x.equals(BigInteger.ZERO);
        } else if (x instanceof Number) {
            double d = ((Number) x).doubleValue();
            return (!Double.isNaN(d) && d != 0.0);
        } else {
            return ScriptRuntime.toBoolean(x);
        }
    }

    private static void doAdd(Object[] stack, double[] sDbl, int stackTop, Context cx) {
        Object rhs = stack[stackTop + 1];
        Object lhs = stack[stackTop];
        double d;
        boolean leftRightOrder;
        if (rhs == DOUBLE_MARK) {
            d = sDbl[stackTop + 1];
            if (lhs == DOUBLE_MARK) {
                sDbl[stackTop] += d;
                return;
            }
            leftRightOrder = true;
            // fallthrough to object + number code
        } else if (lhs == DOUBLE_MARK) {
            d = sDbl[stackTop];
            lhs = rhs;
            leftRightOrder = false;
            // fallthrough to object + number code
        } else {
            if (lhs instanceof Scriptable || rhs instanceof Scriptable) {
                stack[stackTop] = ScriptRuntime.add(lhs, rhs, cx);

                // the next two else if branches are a bit more tricky
                // to reduce method calls
            } else if (lhs instanceof CharSequence) {
                if (rhs instanceof CharSequence) {
                    stack[stackTop] = new ConsString((CharSequence) lhs, (CharSequence) rhs);
                } else {
                    stack[stackTop] =
                            new ConsString((CharSequence) lhs, ScriptRuntime.toCharSequence(rhs));
                }
            } else if (rhs instanceof CharSequence) {
                stack[stackTop] =
                        new ConsString(ScriptRuntime.toCharSequence(lhs), (CharSequence) rhs);

            } else {
                Number lNum = (lhs instanceof Number) ? (Number) lhs : ScriptRuntime.toNumeric(lhs);
                Number rNum = (rhs instanceof Number) ? (Number) rhs : ScriptRuntime.toNumeric(rhs);

                if (lNum instanceof BigInteger && rNum instanceof BigInteger) {
                    stack[stackTop] = ((BigInteger) lNum).add((BigInteger) rNum);
                } else if (lNum instanceof BigInteger || rNum instanceof BigInteger) {
                    throw ScriptRuntime.typeErrorById("msg.cant.convert.to.number", "BigInt");
                } else {
                    stack[stackTop] = DOUBLE_MARK;
                    sDbl[stackTop] = lNum.doubleValue() + rNum.doubleValue();
                }
            }
            return;
        }

        // handle object(lhs) + number(d) code
        if (lhs instanceof Scriptable) {
            rhs = ScriptRuntime.wrapNumber(d);
            if (!leftRightOrder) {
                Object tmp = lhs;
                lhs = rhs;
                rhs = tmp;
            }
            stack[stackTop] = ScriptRuntime.add(lhs, rhs, cx);
        } else if (lhs instanceof CharSequence) {
            CharSequence rstr = ScriptRuntime.numberToString(d, 10);
            if (leftRightOrder) {
                stack[stackTop] = new ConsString((CharSequence) lhs, rstr);
            } else {
                stack[stackTop] = new ConsString(rstr, (CharSequence) lhs);
            }
        } else {
            Number lNum = (lhs instanceof Number) ? (Number) lhs : ScriptRuntime.toNumeric(lhs);
            if (lNum instanceof BigInteger) {
                throw ScriptRuntime.typeErrorById("msg.cant.convert.to.number", "BigInt");
            } else {
                stack[stackTop] = DOUBLE_MARK;
                sDbl[stackTop] = lNum.doubleValue() + d;
            }
        }
    }

    private static int doFastArithemtic(
            CallFrame frame,
            int op,
            double lNum,
            double rNum,
            Object[] stack,
            double[] sDbl,
            int stackTop) {
        stackTop--;

        double result = 0.0;
        switch (op) {
            case Token.SUB:
                result = lNum - rNum;
                break;
            case Token.MUL:
                result = lNum * rNum;
                break;
            case Token.DIV:
                result = lNum / rNum;
                break;
            case Token.MOD:
                result = lNum % rNum;
                break;
            case Token.EXP:
                result = Math.pow(lNum, rNum);
                break;
        }

        stack[stackTop] = DOUBLE_MARK;
        sDbl[stackTop] = result;

        return stackTop;
    }

    private static int doArithmetic(
            CallFrame frame, int op, Object[] stack, double[] sDbl, int stackTop) {
        if (stack[stackTop] == DOUBLE_MARK && stack[stackTop - 1] == DOUBLE_MARK) {
            return doFastArithemtic(
                    frame, op, sDbl[stackTop - 1], sDbl[stackTop], stack, sDbl, stackTop);
        }

        Number lNum = stack_numeric(frame, stackTop - 1);
        Number rNum = stack_numeric(frame, stackTop);
        --stackTop;

        Number result = null;
        switch (op) {
            case Token.SUB:
                result = ScriptRuntime.subtract(lNum, rNum);
                break;
            case Token.MUL:
                result = ScriptRuntime.multiply(lNum, rNum);
                break;
            case Token.DIV:
                result = ScriptRuntime.divide(lNum, rNum);
                break;
            case Token.MOD:
                result = ScriptRuntime.remainder(lNum, rNum);
                break;
            case Token.EXP:
                result = ScriptRuntime.exponentiate(lNum, rNum);
                break;
        }

        if (result instanceof BigInteger) {
            stack[stackTop] = result;
        } else {
            stack[stackTop] = DOUBLE_MARK;
            sDbl[stackTop] = result.doubleValue();
        }
        return stackTop;
    }

    private static Object[] getArgsArray(Object[] stack, double[] sDbl, int shift, int count) {
        return getArgsArray(stack, sDbl, new Object[0], 0, shift, count);
    }

    private static Object[] getArgsArray(
            Object[] stack, double[] sDbl, Object[] bound, int bCount, int shift, int count) {
        if (count == 0) {
            return ScriptRuntime.emptyArgs;
        }
        Object[] args = new Object[count];
        for (int i = 0; i < bCount; i++) {
            args[i] = bound[i];
        }

        for (int i = bCount; i != count; ++i, ++shift) {
            Object val = stack[shift];
            if (val == UniqueTag.DOUBLE_MARK) {
                val = ScriptRuntime.wrapNumber(sDbl[shift]);
            }
            args[i] = val;
        }
        return args;
    }

    private static void addInstructionCount(Context cx, CallFrame frame, int extra) {
        cx.instructionCount += frame.pc - frame.pcPrevBranch + extra;
        if (cx.instructionCount > cx.instructionThreshold) {
            cx.observeInstructionCount(cx.instructionCount);
            cx.instructionCount = 0;
        }
    }
}

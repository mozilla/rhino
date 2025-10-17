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
import org.mozilla.javascript.debug.DebuggableScript;

public final class Interpreter extends Icode implements Evaluator {

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

        final ScriptOrFn<?> fnOrScript;
        final InterpreterData<?> idata;

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
                ScriptOrFn fnOrScript,
                InterpreterData code,
                CallFrame parentFrame,
                CallFrame previousInterpreterFrame) {
            idata = code;
            debuggerFrame =
                    cx.debugger != null
                            ? cx.debugger.getFrame(cx, fnOrScript.getDescriptor())
                            : null;
            useActivation =
                    debuggerFrame != null || fnOrScript.getDescriptor().requiresActivationFrame();

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
            var desc = fnOrScript.getDescriptor();
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

            if (desc.getFunctionType() != 0) {
                scope = fnOrScript.getParentScope();

                if (useActivation) {
                    if (desc.getFunctionType() == FunctionNode.ARROW_FUNCTION) {
                        scope =
                                ScriptRuntime.createArrowFunctionActivation(
                                        (JSFunction) fnOrScript,
                                        cx,
                                        scope,
                                        args,
                                        desc.isStrict(),
                                        desc.hasRestArg(),
                                        desc.requiresArgumentObject());
                    } else {
                        scope =
                                ScriptRuntime.createFunctionActivation(
                                        (JSFunction) fnOrScript,
                                        cx,
                                        scope,
                                        args,
                                        desc.isStrict(),
                                        desc.hasRestArg(),
                                        desc.requiresArgumentObject());
                    }
                }
            } else {
                scope = callerScope;
                ScriptRuntime.initScript(fnOrScript, thisObj, cx, scope, desc.isEvalFunction());
            }

            if (desc.getFunctionCount() != 0) {
                if (desc.getFunctionType() != 0 && !desc.requiresActivationFrame()) Kit.codeBug();
                for (int i = 0; i < desc.getFunctionCount(); i++) {
                    JSDescriptor fdesc = desc.getFunction(i);
                    if (fdesc.getFunctionType() == FunctionNode.FUNCTION_STATEMENT) {
                        initFunction(cx, scope, fnOrScript.getDescriptor(), i);
                    }
                }
            }

            int varCount = desc.getParamAndVarCount();
            for (int i = 0; i < varCount; i++) {
                if (desc.getParamOrVarConst(i)) stackAttributes[i] = ScriptableObject.CONST;
            }
            int definedArgs = desc.getParamCount();
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

            if (desc.hasRestArg()) {
                Object[] vals;
                int offset = desc.getParamCount() - 1;
                if (argCount >= desc.getParamCount()) {
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
                                            (c, scope, thisObj) -> equalsInTopScope(other),
                                            cx,
                                            top,
                                            top,
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
                    return f.fnOrScript.getDescriptor().isStrict();
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
                    && compareDescs(fnOrScript.getDescriptor(), other.fnOrScript.getDescriptor())
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

    private static boolean compareDescs(JSDescriptor i1, JSDescriptor i2) {
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

    private static class CompilationResult<T extends ScriptOrFn<T>> {
        private final JSDescriptor<T> descriptor;
        private final Scriptable homeObject;

        CompilationResult(JSDescriptor<T> descriptor, Scriptable homeObject) {
            this.descriptor = descriptor;
            this.homeObject = homeObject;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object compile(
            CompilerEnvirons compilerEnv,
            ScriptNode tree,
            String rawSource,
            boolean returnFunction) {
        CodeGenerator<?> cgen = new CodeGenerator<>();
        var itsData = cgen.compile(compilerEnv, tree, rawSource, returnFunction);
        return new CompilationResult(itsData, compilerEnv.homeObject());
    }

    @Override
    @SuppressWarnings("unchecked")
    public DebuggableScript getDebuggableScript(Object bytecode) {
        return ((CompilationResult<?>) bytecode).descriptor;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Script createScriptObject(Object bytecode, Object staticSecurityDomain) {
        var compilerResult = (CompilationResult<JSScript>) bytecode;
        return JSFunction.createScript(
                compilerResult.descriptor, compilerResult.homeObject, staticSecurityDomain);
    }

    @Override
    public void setEvalScriptFlag(Script script) {
        throw new UnsupportedOperationException();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Function createFunctionObject(
            Context cx, Scriptable scope, Object bytecode, Object staticSecurityDomain) {
        var compilerResult = (CompilationResult<JSFunction>) bytecode;
        return JSFunction.createFunction(
                cx,
                scope,
                compilerResult.descriptor,
                compilerResult.homeObject,
                staticSecurityDomain);
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

    static PrintStream interpreterBytecodePrintStream = System.out;

    static <T extends ScriptOrFn<T>> void dumpICode(
            InterpreterData.Builder<T> idata, JSDescriptor.Builder<T> desc) {
        if (!Token.printICode) {
            return;
        }

        byte[] iCode = idata.itsICode;
        int iCodeLength = iCode.length;
        String[] strings = idata.itsStringTable;
        BigInteger[] bigInts = idata.itsBigIntTable;
        PrintStream out = interpreterBytecodePrintStream;
        out.println("ICode dump, for " + desc.name + ", length = " + iCodeLength);
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
                        if (indexReg < 0) {
                            out.println(tname + " length: " + (-indexReg - 1));
                        } else {
                            Object[] keys = (Object[]) idata.literalIds[indexReg];
                            out.println(tname + " " + Arrays.toString(keys) + " " + copyArray);
                        }
                        break;
                    }
                case Icode_SPARE_ARRAYLIT:
                    out.println(tname + " " + idata.literalIds[indexReg]);
                    break;
                case Icode_CLOSURE_EXPR:
                case Icode_CLOSURE_STMT:
                case Icode_METHOD_EXPR:
                    out.println(tname + " #" + indexReg);
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
                case Icode_REG_BIGINT_C0:
                    out.println(tname + " " + bigInts[0].toString() + 'n');
                    break;
                case Icode_REG_BIGINT_C1:
                    out.println(tname + " " + bigInts[1].toString() + 'n');
                    break;
                case Icode_REG_BIGINT_C2:
                    out.println(tname + " " + bigInts[2].toString() + 'n');
                    break;
                case Icode_REG_BIGINT_C3:
                    out.println(tname + " " + bigInts[3].toString() + 'n');
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

            case Icode_REG_BIGINT1:
                // ubyte bigint index
                return 1 + 1;

            case Icode_REG_BIGINT2:
                // ushort bigint index
                return 1 + 2;

            case Icode_REG_BIGINT4:
                // uint bigint index
                return 1 + 4;
        }
        if (!validBytecode(bytecode)) throw Kit.codeBug();
        return 1;
    }

    static int[] getLineNumbers(JSDescriptor desc) {
        JSCode code = desc.getCode();
        InterpreterData data;
        if (code instanceof InterpreterData) {
            data = (InterpreterData) code;
        } else {
            code = desc.getConstructor();
            if (code instanceof InterpreterData) {
                data = (InterpreterData) code;
            } else {
                Kit.codeBug("Attempt to get line number data for non-interpreted code.");
                return null;
            }
        }

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
            ex.interpreterLineData = ((CallFrame) cx.lastInterpreterFrame).pcSourceLineStart;
        }
    }

    @Override
    public String getSourcePositionFromStack(Context cx, int[] linep) {
        CallFrame frame = (CallFrame) cx.lastInterpreterFrame;
        InterpreterData idata = frame.idata;
        JSDescriptor desc = frame.fnOrScript.getDescriptor();
        if (frame.pcSourceLineStart >= 0) {
            linep[0] = getIndex(idata.itsICode, frame.pcSourceLineStart);
        } else {
            linep[0] = 0;
        }
        return desc.getSourceName();
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
                JSDescriptor desc = callerFrame.fnOrScript.getDescriptor();
                sb.append(lineSeparator);
                sb.append("\tat script");
                if (desc.getName() != null && desc.getName().length() != 0) {
                    sb.append('.');
                    sb.append(desc.getName());
                }
                sb.append('(');
                sb.append(desc.getSourceName());
                int pc = calleeFrame == null ? ex.interpreterLineData : calleeFrame.parentPC;
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
                InterpreterData idata = callerFrame.idata;
                JSDescriptor desc = callerFrame.fnOrScript.getDescriptor();
                String fileName = desc.getSourceName();
                String functionName = null;
                int lineNumber = -1;
                int pc = calleeFrame == null ? ex.interpreterLineData : calleeFrame.parentPC;
                if (pc >= 0) {
                    lineNumber = getIndex(idata.itsICode, pc);
                }
                if (desc.getName() != null && desc.getName().length() != 0) {
                    functionName = desc.getName();
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

    static String getRawSource(JSDescriptor desc) {
        if (desc.getRawSource() == null) {
            return null;
        }
        return desc.getRawSource();
    }

    private static void initFunction(Context cx, Scriptable scope, JSDescriptor parent, int index) {
        JSFunction fn;
        fn = JSFunction.createFunction(cx, scope, parent, index, null);
        var desc = fn.getDescriptor();
        ScriptRuntime.initFunction(cx, scope, fn, desc.getFunctionType(), parent.isEvalFunction());
    }

    static Object interpret(
            ScriptOrFn ifun,
            InterpreterData idata,
            Context cx,
            Scriptable scope,
            Scriptable thisObj,
            Object[] args) {
        if (!ScriptRuntime.hasTopCall(cx)) Kit.codeBug();

        var desc = ifun.getDescriptor();

        if (cx.interpreterSecurityDomain != desc.getSecurityDomain()) {
            Object savedDomain = cx.interpreterSecurityDomain;
            cx.interpreterSecurityDomain = desc.getSecurityDomain();
            try {
                if (ifun instanceof JSScript) {
                    return desc.getSecurityController()
                            .callWithDomain(
                                    desc.getSecurityDomain(),
                                    cx,
                                    (JSScript) ifun,
                                    scope,
                                    thisObj,
                                    args);
                } else if (ifun instanceof JSFunction) {
                    return desc.getSecurityController()
                            .callWithDomain(
                                    desc.getSecurityDomain(),
                                    cx,
                                    (JSFunction) ifun,
                                    scope,
                                    thisObj,
                                    args);
                } else {
                    Kit.codeBug("Unknown compiled code type.");
                }
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
                        idata,
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
                } catch (NativeGenerator.GeneratorClosedException e) {
                    // Re-throw GeneratorClosedException so ES6Generator can catch and complete
                    throw e;
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
        instructionObjs[base + Icode_GENERATOR] = new DoGenerator();
        instructionObjs[base + Token.YIELD] = new DoYield();
        instructionObjs[base + Icode_YIELD_STAR] = new DoYield();
        instructionObjs[base + Icode_GENERATOR_END] = new DoGeneratorEnd();
        instructionObjs[base + Icode_GENERATOR_RETURN] = new DoGeneratorReturn();
        instructionObjs[base + Token.THROW] = new DoThrow();
        instructionObjs[base + Token.RETHROW] = new DoRethrow();
        instructionObjs[base + Token.GE] = new DoCompare();
        instructionObjs[base + Token.LE] = new DoCompare();
        instructionObjs[base + Token.GT] = new DoCompare();
        instructionObjs[base + Token.LT] = new DoCompare();
        instructionObjs[base + Token.IN] = new DoInOrInstanceof();
        instructionObjs[base + Token.INSTANCEOF] = new DoInOrInstanceof();
        instructionObjs[base + Token.EQ] = new DoEquals();
        instructionObjs[base + Token.NE] = new DoNotEquals();
        instructionObjs[base + Token.SHEQ] = new DoShallowEquals();
        instructionObjs[base + Token.SHNE] = new DoShallowNotEquals();
        instructionObjs[base + Token.IFNE] = new DoIfNE();
        instructionObjs[base + Token.IFEQ] = new DoIfEQ();
        instructionObjs[base + Icode_IFEQ_POP] = new DoIfEQPop();
        instructionObjs[base + Icode_IF_NULL_UNDEF] = new DoIfNullUndef();
        instructionObjs[base + Icode_IF_NOT_NULL_UNDEF] = new DoIfNotNullUndef();
        instructionObjs[base + Token.GOTO] = new DoGoto();
        instructionObjs[base + Icode_GOSUB] = new DoGosub();
        instructionObjs[base + Icode_STARTSUB] = new DoStartSub();
        instructionObjs[base + Icode_RETSUB] = new DoRetsub();
        instructionObjs[base + Icode_POP] = new DoPop();
        instructionObjs[base + Icode_POP_RESULT] = new DoPopResult();
        instructionObjs[base + Icode_DUP] = new DoDup();
        instructionObjs[base + Icode_DUP2] = new DoDup2();
        instructionObjs[base + Icode_SWAP] = new DoSwap();
        instructionObjs[base + Token.RETURN] = new DoReturn();
        instructionObjs[base + Token.RETURN_RESULT] = new DoReturnResult();
        instructionObjs[base + Icode_RETUNDEF] = new DoReturnUndef();
        instructionObjs[base + Token.BITNOT] = new DoBitNot();
        instructionObjs[base + Token.BITAND] = new DoBitOp();
        instructionObjs[base + Token.BITOR] = new DoBitOp();
        instructionObjs[base + Token.BITXOR] = new DoBitOp();
        instructionObjs[base + Token.LSH] = new DoBitOp();
        instructionObjs[base + Token.RSH] = new DoBitOp();
        instructionObjs[base + Token.URSH] = new DoUnsignedRightShift();
        instructionObjs[base + Token.POS] = new DoPositive();
        instructionObjs[base + Token.NEG] = new DoNegative();
        instructionObjs[base + Token.ADD] = new DoAdd();
        instructionObjs[base + Token.SUB] = new DoArithmetic();
        instructionObjs[base + Token.MUL] = new DoArithmetic();
        instructionObjs[base + Token.DIV] = new DoArithmetic();
        instructionObjs[base + Token.MOD] = new DoArithmetic();
        instructionObjs[base + Token.EXP] = new DoArithmetic();
        instructionObjs[base + Token.NOT] = new DoNot();
        instructionObjs[base + Token.BINDNAME] = new DoBindName();
        instructionObjs[base + Token.STRICT_SETNAME] = new DoSetName();
        instructionObjs[base + Token.STRING_CONCAT] = new DoStringConcat();
        instructionObjs[base + Token.SETNAME] = new DoSetName();
        instructionObjs[base + Icode_SETCONST] = new DoSetConst();
        instructionObjs[base + Token.DELPROP] = new DoDelName();
        instructionObjs[base + Icode_DELNAME] = new DoDelName();
        instructionObjs[base + Icode_DELPROP_SUPER] = new DoDelPropSuper();
        instructionObjs[base + Token.GETPROPNOWARN] = new DoGetPropNoWarn();
        instructionObjs[base + Token.GETPROP] = new DoGetProp();
        instructionObjs[base + Token.GETPROP_SUPER] = new DoGetPropSuper();
        instructionObjs[base + Token.GETPROPNOWARN_SUPER] = new DoGetPropSuper();
        instructionObjs[base + Token.SETPROP] = new DoSetProp();
        instructionObjs[base + Token.SETPROP_SUPER] = new DoSetPropSuper();
        instructionObjs[base + Icode_PROP_INC_DEC] = new DoPropIncDec();
        instructionObjs[base + Token.GETELEM] = new DoGetElem();
        instructionObjs[base + Token.GETELEM_SUPER] = new DoGetElemSuper();
        instructionObjs[base + Token.SETELEM] = new DoSetElem();
        instructionObjs[base + Token.SETELEM_SUPER] = new DoSetElemSuper();
        instructionObjs[base + Icode_ELEM_INC_DEC] = new DoElemIncDec();
        instructionObjs[base + Token.GET_REF] = new DoGetRef();
        instructionObjs[base + Token.SET_REF] = new DoSetRef();
        instructionObjs[base + Token.DEL_REF] = new DoDelRef();
        instructionObjs[base + Icode_REF_INC_DEC] = new DoRefIncDec();
        instructionObjs[base + Token.LOCAL_LOAD] = new DoLocalLoad();
        instructionObjs[base + Icode_LOCAL_CLEAR] = new DoLocalClear();
        instructionObjs[base + Icode_NAME_AND_THIS] = new DoNameAndThis();
        instructionObjs[base + Icode_NAME_AND_THIS_OPTIONAL] = new DoNameAndThisOptional();
        instructionObjs[base + Icode_PROP_AND_THIS] = new DoPropAndThis();
        instructionObjs[base + Icode_PROP_AND_THIS_OPTIONAL] = new DoPropAndThisOptional();
        instructionObjs[base + Icode_ELEM_AND_THIS] = new DoElemAndThis();
        instructionObjs[base + Icode_ELEM_AND_THIS_OPTIONAL] = new DoElemAndThisOptional();
        instructionObjs[base + Icode_VALUE_AND_THIS] = new DoValueAndThis();
        instructionObjs[base + Icode_VALUE_AND_THIS_OPTIONAL] = new DoValueAndThisOptional();
        instructionObjs[base + Icode_CALLSPECIAL] = new DoCallSpecial();
        instructionObjs[base + Icode_CALLSPECIAL_OPTIONAL] = new DoCallSpecial();
        instructionObjs[base + Token.CALL] = new DoCallByteCode();
        instructionObjs[base + Icode_CALL_ON_SUPER] = new DoCallByteCode();
        instructionObjs[base + Icode_TAIL_CALL] = new DoCallByteCode();
        instructionObjs[base + Token.REF_CALL] = new DoCallByteCode();
        instructionObjs[base + Token.NEW] = new DoNew();
        instructionObjs[base + Token.TYPEOF] = new DoTypeOf();
        instructionObjs[base + Icode_TYPEOFNAME] = new DoTypeOfName();
        instructionObjs[base + Token.STRING] = new DoString();
        instructionObjs[base + Icode_SHORTNUMBER] = new DoShortNumber();
        instructionObjs[base + Icode_INTNUMBER] = new DoIntNumber();
        instructionObjs[base + Token.NUMBER] = new DoNumber();
        instructionObjs[base + Token.BIGINT] = new DoBigInt();
        instructionObjs[base + Token.NAME] = new DoName();
        instructionObjs[base + Icode_NAME_INC_DEC] = new DoNameIncDec();
        instructionObjs[base + Icode_SETCONSTVAR1] = new DoSetConstVar1();
        instructionObjs[base + Icode_SETCONSTVAR] = new DoSetConstVar();
        instructionObjs[base + Icode_SETVAR1] = new DoSetVar1();
        instructionObjs[base + Token.SETVAR] = new DoSetVar();
        instructionObjs[base + Icode_GETVAR1] = new DoGetVar1();
        instructionObjs[base + Token.GETVAR] = new DoGetVar();
        instructionObjs[base + Icode_VAR_INC_DEC] = new DoVarIncDec();
        instructionObjs[base + Icode_ZERO] = new DoZero();
        instructionObjs[base + Icode_ONE] = new DoOne();
        instructionObjs[base + Token.NULL] = new DoNull();
        instructionObjs[base + Token.THIS] = new DoThis();
        instructionObjs[base + Token.SUPER] = new DoSuper();
        instructionObjs[base + Token.THISFN] = new DoThisFunction();
        instructionObjs[base + Token.FALSE] = new DoFalse();
        instructionObjs[base + Token.TRUE] = new DoTrue();
        instructionObjs[base + Icode_UNDEF] = new DoUndef();
        instructionObjs[base + Token.ENTERWITH] = new DoEnterWith();
        instructionObjs[base + Token.LEAVEWITH] = new DoLeaveWith();
        instructionObjs[base + Token.CATCH_SCOPE] = new DoCatchScope();
        instructionObjs[base + Token.ENUM_INIT_KEYS] = new DoEnumInit();
        instructionObjs[base + Token.ENUM_INIT_VALUES] = new DoEnumInit();
        instructionObjs[base + Token.ENUM_INIT_ARRAY] = new DoEnumInit();
        instructionObjs[base + Token.ENUM_INIT_VALUES_IN_ORDER] = new DoEnumInit();
        instructionObjs[base + Token.ENUM_INIT_VALUES_IN_ORDER] = new DoEnumInit();
        instructionObjs[base + Token.ENUM_NEXT] = new DoEnumOp();
        instructionObjs[base + Token.ENUM_ID] = new DoEnumOp();
        instructionObjs[base + Token.REF_SPECIAL] = new DoRefSpecial();
        instructionObjs[base + Token.REF_MEMBER] = new DoRefMember();
        instructionObjs[base + Token.REF_NS_MEMBER] = new DoRefNsMember();
        instructionObjs[base + Token.REF_NAME] = new DoRefName();
        instructionObjs[base + Token.REF_NS_NAME] = new DoRefNsName();
        instructionObjs[base + Icode_SCOPE_LOAD] = new DoScopeLoad();
        instructionObjs[base + Icode_SCOPE_SAVE] = new DoScopeSave();
        instructionObjs[base + Icode_SPREAD] = new DoSpread();
        instructionObjs[base + Icode_CLOSURE_EXPR] = new DoClosureExpr();
        instructionObjs[base + Icode_METHOD_EXPR] = new DoMethodExpr();
        instructionObjs[base + Icode_CLOSURE_STMT] = new DoClosureStatement();
        instructionObjs[base + Token.REGEXP] = new DoRegExp();
        instructionObjs[base + Icode_TEMPLATE_LITERAL_CALLSITE] = new DoTemplateLiteralCallSite();
        instructionObjs[base + Icode_LITERAL_NEW_OBJECT] = new DoLiteralNewObject();
        instructionObjs[base + Icode_LITERAL_NEW_ARRAY] = new DoLiteralNewArray();
        instructionObjs[base + Icode_LITERAL_SET] = new DoLiteralSet();
        instructionObjs[base + Icode_LITERAL_GETTER] = new DoLiteralGetter();
        instructionObjs[base + Icode_LITERAL_SETTER] = new DoLiteralSetter();
        instructionObjs[base + Icode_LITERAL_KEY_SET] = new DoLiteralKeySet();
        instructionObjs[base + Token.OBJECTLIT] = new DoObjectLit();
        instructionObjs[base + Token.ARRAYLIT] = new DoArrayLiteral();
        instructionObjs[base + Icode_SPARE_ARRAYLIT] = new DoArrayLiteral();
        instructionObjs[base + Icode_ENTERDQ] = new DoEnterDotQuery();
        instructionObjs[base + Icode_LEAVEDQ] = new DoLeaveDotQuery();
        instructionObjs[base + Token.DEFAULTNAMESPACE] = new DoDefaultNamespace();
        instructionObjs[base + Token.ESCXMLATTR] = new DoEscXMLAttr();
        instructionObjs[base + Token.ESCXMLTEXT] = new DoEscXMLText();
        instructionObjs[base + Icode_DEBUGGER] = new DoDebug();
        instructionObjs[base + Icode_LINE] = new DoLineChange();
        instructionObjs[base + Icode_REG_IND_C0] = new DoIndexCn();
        instructionObjs[base + Icode_REG_IND_C1] = new DoIndexCn();
        instructionObjs[base + Icode_REG_IND_C2] = new DoIndexCn();
        instructionObjs[base + Icode_REG_IND_C3] = new DoIndexCn();
        instructionObjs[base + Icode_REG_IND_C4] = new DoIndexCn();
        instructionObjs[base + Icode_REG_IND_C5] = new DoIndexCn();
        instructionObjs[base + Icode_REG_IND1] = new DoRegIndex1();
        instructionObjs[base + Icode_REG_IND2] = new DoRegIndex2();
        instructionObjs[base + Icode_REG_IND4] = new DoRegIndex4();
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
            Object tble,
            GeneratorState genState,
            int iReg,
            boolean instructionCounting) {
        final InterpreterState state =
                new InterpreterState(frame.savedStackTop, iReg, instructionCounting);

        withoutExceptions:
        try {

            // Use local variables for constant values in frame
            // for faster access
            final byte[] iCode = frame.idata.itsICode;

            state.generatorState = genState;
            state.throwable = tble;

            // Store new frame in cx which is used for error reporting etc.
            cx.lastInterpreterFrame = frame;

            Loop:
            for (; ; ) {

                NewState nextState;
                do {
                    // Exception handler assumes that PC is already incremented
                    // pass the instruction start when it searches the
                    // exception handler
                    int op = iCode[frame.pc++];

                    var insn = instructionObjs[-MIN_ICODE + op];

                    nextState = insn.execute(cx, frame, state, op);
                } while (nextState == null);

                if (nextState == BREAK_LOOP) {
                    break Loop;
                } else if (nextState == BREAK_JUMPLESSRUN) {
                    if (instructionCounting) {
                        addInstructionCount(cx, frame, 2);
                    }
                    int offset = getShort(iCode, frame.pc);
                    if (offset != 0) {
                        // -1 accounts for pc pointing to jump opcode + 1
                        frame.pc += offset - 1;
                    } else {
                        frame.pc = frame.idata.longJumps.get(frame.pc);
                    }
                    if (instructionCounting) {
                        frame.pcPrevBranch = frame.pc;
                    }
                } else if (nextState == BREAK_WITHOUT_EXTENSION) {
                    break withoutExceptions;
                } else {
                    return (InterpreterResult) nextState;
                }
            }

            exitFrame(cx, frame, null);
            if (frame.parentFrame != null) {
                CallFrame newFrame = frame.parentFrame;
                if (newFrame.frozen) {
                    newFrame = newFrame.cloneFrozen();
                }
                setCallResult(newFrame, frame.result, frame.resultDbl);
                return new StateContinueResult(newFrame, state.indexReg);
            }
            return new StateBreakResult(frame);

        } // end of interpreter withoutExceptions: try
        catch (Throwable ex) {
            if (state.throwable != null) {
                // This is serious bug and it is better to track it ASAP
                ex.printStackTrace(System.err);
                throw new IllegalStateException();
            }
            state.throwable = ex;
        }
        return new ThrowableResult(frame, state.throwable);
    }

    private static class DoGenerator extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            if (!frame.frozen) {
                // First time encountering this opcode: create new generator
                // object and return
                generatorCreate(cx, frame);
                return BREAK_LOOP;
            }
            /* This is both where we yield from and re-enter the
             * generator.
             */
            if (!frame.frozen) {
                return new YieldResult(
                        freezeGenerator(
                                cx, frame, state, state.generatorState, op == Icode_YIELD_STAR));
            }
            Object obj = thawGenerator(frame, state, state.generatorState, op);
            if (obj != Scriptable.NOT_FOUND) {
                state.throwable = obj;
                return BREAK_WITHOUT_EXTENSION;
            }
            return null;
        }

        private static void generatorCreate(Context cx, CallFrame frame) {
            // First time encountering this opcode: create new generator
            // object and return
            frame.pc--; // we want to come back here when we resume
            CallFrame generatorFrame = captureFrameForGenerator(frame);
            generatorFrame.frozen = true;
            if (cx.getLanguageVersion() >= Context.VERSION_ES6) {
                frame.result =
                        new ES6Generator(
                                frame.scope,
                                (JSFunction) generatorFrame.fnOrScript,
                                generatorFrame);
            } else {
                frame.result =
                        new NativeGenerator(
                                frame.scope,
                                (JSFunction) generatorFrame.fnOrScript,
                                generatorFrame);
            }
        }
    }

    private static Object freezeGenerator(
            Context cx,
            CallFrame frame,
            InterpreterState state,
            GeneratorState generatorState,
            boolean yieldStar) {
        if (generatorState.operation == NativeGenerator.GENERATOR_CLOSE) {
            // Error: no yields when generator is closing
            throw ScriptRuntime.typeErrorById("msg.yield.closing");
        }
        // return to our caller (which should be a method of NativeGenerator)
        frame.frozen = true;
        frame.result = frame.stack[state.stackTop];
        frame.resultDbl = frame.sDbl[state.stackTop];
        frame.savedStackTop = state.stackTop;
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
            CallFrame frame, InterpreterState state, GeneratorState generatorState, int op) {
        // we are resuming execution
        frame.frozen = false;
        int sourceLine = getIndex(frame.idata.itsICode, frame.pc);
        frame.pc += 2; // skip line number data
        if (generatorState.operation == NativeGenerator.GENERATOR_THROW) {
            // processing a call to <generator>.throw(exception): must
            // act as if exception was thrown from resumption point.
            return new JavaScriptException(
                    generatorState.value,
                    frame.fnOrScript.getDescriptor().getSourceName(),
                    sourceLine);
        }
        if (generatorState.operation == NativeGenerator.GENERATOR_CLOSE) {
            return generatorState.value;
        }
        if (generatorState.operation != NativeGenerator.GENERATOR_SEND) throw Kit.codeBug();
        if ((op == Token.YIELD) || (op == Icode_YIELD_STAR)) {
            frame.stack[state.stackTop] = generatorState.value;
        }
        return Scriptable.NOT_FOUND;
    }

    private static class DoYield extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            /* This is both where we yield from and re-enter the
             * generator.
             */
            if (!frame.frozen) {
                return new YieldResult(
                        freezeGenerator(
                                cx, frame, state, state.generatorState, op == Icode_YIELD_STAR));
            }
            Object obj = thawGenerator(frame, state, state.generatorState, op);
            if (obj != Scriptable.NOT_FOUND) {
                state.throwable = obj;
                return BREAK_WITHOUT_EXTENSION;
            }
            return null;
        }
    }

    private static class DoGeneratorEnd extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            // throw StopIteration
            frame.frozen = true;
            int sourceLine = getIndex(frame.idata.itsICode, frame.pc);
            state.generatorState.returnedException =
                    new JavaScriptException(
                            NativeIterator.getStopIterationObject(frame.scope),
                            frame.fnOrScript.getDescriptor().getSourceName(),
                            sourceLine);
            return BREAK_LOOP;
        }
    }

    private static class DoGeneratorReturn extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            // throw StopIteration with the value of "return"
            frame.frozen = true;
            frame.result = frame.stack[state.stackTop];
            frame.resultDbl = frame.sDbl[state.stackTop--];

            NativeIterator.StopIteration si =
                    new NativeIterator.StopIteration(
                            (frame.result == DOUBLE_MARK)
                                    ? Double.valueOf(frame.resultDbl)
                                    : frame.result);

            int sourceLine = getIndex(frame.idata.itsICode, frame.pc);
            state.generatorState.returnedException =
                    new JavaScriptException(
                            si, frame.fnOrScript.getDescriptor().getSourceName(), sourceLine);
            return BREAK_LOOP;
        }
    }

    private static class DoRethrow extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            state.indexReg += frame.idata.itsMaxVars;
            state.throwable = frame.stack[state.indexReg];
            return BREAK_WITHOUT_EXTENSION;
        }
    }

    private static class DoThrow extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            state.throwable =
                    throwObject(
                            frame,
                            frame.stack,
                            frame.sDbl,
                            frame.idata,
                            frame.idata.itsICode,
                            state);
            --state.stackTop;
            return BREAK_WITHOUT_EXTENSION;
        }

        private static Object throwObject(
                CallFrame frame,
                final Object[] stack,
                final double[] sDbl,
                final InterpreterData iData,
                final byte[] iCode,
                InterpreterState state) {
            Object throwable;
            Object value = stack[state.stackTop];
            if (value == DOUBLE_MARK) value = ScriptRuntime.wrapNumber(sDbl[state.stackTop]);

            int sourceLine = getIndex(iCode, frame.pc);
            throwable =
                    new JavaScriptException(
                            value, frame.fnOrScript.getDescriptor().getSourceName(), sourceLine);
            return throwable;
        }
    }

    private static class DoCompare extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            Object[] stack = frame.stack;
            double[] sDbl = frame.sDbl;
            Object rhs = stack[state.stackTop];
            Object lhs = stack[--state.stackTop];
            boolean valBln;
            if (lhs == DOUBLE_MARK && rhs == DOUBLE_MARK) {
                valBln =
                        ScriptRuntime.compareTo(sDbl[state.stackTop], sDbl[state.stackTop + 1], op);
                stack[state.stackTop] = valBln;
                return null;
            }
            object_compare:
            {
                number_compare:
                {
                    Number rNum, lNum;
                    if (rhs == DOUBLE_MARK) {
                        rNum = sDbl[state.stackTop + 1];
                        lNum = stack_numeric(frame, state.stackTop);
                    } else if (lhs == DOUBLE_MARK) {
                        rNum = ScriptRuntime.toNumeric(rhs);
                        lNum = sDbl[state.stackTop];
                    } else {
                        break number_compare;
                    }
                    valBln = ScriptRuntime.compare(lNum, rNum, op);
                    break object_compare;
                }
                valBln = ScriptRuntime.compare(lhs, rhs, op);
            }
            stack[state.stackTop] = valBln;
            return null;
        }
    }

    private static class DoInOrInstanceof extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            Object[] stack = frame.stack;
            double[] sDbl = frame.sDbl;
            Object rhs = stack[state.stackTop];
            if (rhs == DOUBLE_MARK) rhs = ScriptRuntime.wrapNumber(sDbl[state.stackTop]);
            Object lhs = stack[--state.stackTop];
            if (lhs == DOUBLE_MARK) lhs = ScriptRuntime.wrapNumber(sDbl[state.stackTop]);
            boolean valBln;
            if (op == Token.IN) {
                valBln = ScriptRuntime.in(lhs, rhs, cx);
            } else {
                valBln = ScriptRuntime.instanceOf(lhs, rhs, cx);
            }
            stack[state.stackTop] = valBln;
            return null;
        }
    }

    private static class DoEquals extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            Object[] stack = frame.stack;
            double[] sDbl = frame.sDbl;
            final boolean res = doEquals(state, stack, sDbl);
            stack[state.stackTop] = res;
            return null;
        }
    }

    private static class DoNotEquals extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            Object[] stack = frame.stack;
            double[] sDbl = frame.sDbl;
            final boolean res = doEquals(state, stack, sDbl);
            stack[state.stackTop] = !res;
            return null;
        }
    }

    private static boolean doEquals(InterpreterState state, Object[] stack, double[] sDbl) {
        final Object rhs = stack[state.stackTop--];
        final Object lhs = stack[state.stackTop];
        final boolean res;
        if (rhs == DOUBLE_MARK) {
            if (lhs == DOUBLE_MARK) {
                res = (sDbl[state.stackTop] == sDbl[state.stackTop + 1]);
            } else {
                res = ScriptRuntime.eqNumber(sDbl[state.stackTop + 1], lhs);
            }
        } else if (lhs == DOUBLE_MARK) {
            res = ScriptRuntime.eqNumber(sDbl[state.stackTop], rhs);
        } else {
            res = ScriptRuntime.eq(lhs, rhs);
        }
        return res;
    }

    private static class DoShallowEquals extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            Object[] stack = frame.stack;
            double[] sDbl = frame.sDbl;
            final boolean res = doShallowEquals(state, stack, sDbl);
            stack[state.stackTop] = res;
            return null;
        }
    }

    private static class DoShallowNotEquals extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            Object[] stack = frame.stack;
            double[] sDbl = frame.sDbl;
            final boolean res = doShallowEquals(state, stack, sDbl);
            stack[state.stackTop] = !res;
            return null;
        }
    }

    private static boolean doShallowEquals(InterpreterState state, Object[] stack, double[] sDbl) {
        final Object rhs = stack[state.stackTop--];
        final Object lhs = stack[state.stackTop];
        final boolean res;
        if (rhs == DOUBLE_MARK) {
            double rDbl = sDbl[state.stackTop + 1];
            if (lhs == DOUBLE_MARK) {
                res = rDbl == sDbl[state.stackTop];
            } else if (lhs instanceof Number && !(lhs instanceof BigInteger)) {
                res = rDbl == ((Number) lhs).doubleValue();
            } else {
                res = false;
            }
        } else if (lhs == DOUBLE_MARK) {
            double ldbl = sDbl[state.stackTop];
            if (rhs instanceof Number && !(rhs instanceof BigInteger)) {
                res = ldbl == ((Number) rhs).doubleValue();
            } else {
                res = false;
            }
        } else {
            res = ScriptRuntime.shallowEq(lhs, rhs);
        }
        return res;
    }

    private static class DoIfNE extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            if (stack_boolean(frame, state.stackTop--)) {
                frame.pc += 2;
                return null;
            }
            return BREAK_JUMPLESSRUN;
        }
    }

    private static class DoIfEQ extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            if (!stack_boolean(frame, state.stackTop--)) {
                frame.pc += 2;
                return null;
            }
            return BREAK_JUMPLESSRUN;
        }
    }

    private static class DoIfEQPop extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            if (!stack_boolean(frame, state.stackTop--)) {
                frame.pc += 2;
                return null;
            }
            frame.stack[state.stackTop--] = null;
            return BREAK_JUMPLESSRUN;
        }
    }

    private static class DoIfNullUndef extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            Object val = frame.stack[state.stackTop];
            --state.stackTop;
            if (val != null && !Undefined.isUndefined(val)) {
                frame.pc += 2;
                return null;
            }
            return BREAK_JUMPLESSRUN;
        }
    }

    private static class DoIfNotNullUndef extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            Object val = frame.stack[state.stackTop];
            --state.stackTop;
            if (val == null || Undefined.isUndefined(val)) {
                frame.pc += 2;
                return null;
            }
            return BREAK_JUMPLESSRUN;
        }
    }

    private static class DoGoto extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            return BREAK_JUMPLESSRUN;
        }
    }

    private static class DoGosub extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            ++state.stackTop;
            frame.stack[state.stackTop] = DOUBLE_MARK;
            frame.sDbl[state.stackTop] = frame.pc + 2;
            return BREAK_JUMPLESSRUN;
        }
    }

    private static class DoStartSub extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            final Object[] stack = frame.stack;
            final double[] sDbl = frame.sDbl;
            final InterpreterData iData = frame.idata;
            if (state.stackTop == frame.emptyStackTop + 1) {
                // Call from Icode_GOSUB: store return PC address in the local
                state.indexReg += iData.itsMaxVars;
                stack[state.indexReg] = stack[state.stackTop];
                sDbl[state.indexReg] = sDbl[state.stackTop];
                --state.stackTop;
            } else {
                // Call from exception handler: exception object is already
                // stored
                // in the local
                if (state.stackTop != frame.emptyStackTop) Kit.codeBug();
            }
            return null;
        }
    }

    private static class DoRetsub extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            // state.indexReg: local to store return address
            if (state.instructionCounting) {
                addInstructionCount(cx, frame, 0);
            }
            state.indexReg += frame.idata.itsMaxVars;
            Object value = frame.stack[state.indexReg];
            if (value != DOUBLE_MARK) {
                // Invocation from exception handler, restore object to
                // rethrow
                state.throwable = value;
                return BREAK_WITHOUT_EXTENSION;
            }
            // Normal return from GOSUB
            frame.pc = (int) frame.sDbl[state.indexReg];
            if (state.instructionCounting) {
                frame.pcPrevBranch = frame.pc;
            }
            return null;
        }
    }

    private static class DoPop extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            final Object[] stack = frame.stack;
            stack[state.stackTop] = null;
            state.stackTop--;
            return null;
        }
    }

    private static class DoPopResult extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            final Object[] stack = frame.stack;
            final double[] sDbl = frame.sDbl;
            frame.result = stack[state.stackTop];
            frame.resultDbl = sDbl[state.stackTop];
            stack[state.stackTop] = null;
            --state.stackTop;
            return null;
        }
    }

    private static class DoDup extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            final Object[] stack = frame.stack;
            final double[] sDbl = frame.sDbl;
            stack[state.stackTop + 1] = stack[state.stackTop];
            sDbl[state.stackTop + 1] = sDbl[state.stackTop];
            state.stackTop++;
            return null;
        }
    }

    private static class DoDup2 extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            final Object[] stack = frame.stack;
            final double[] sDbl = frame.sDbl;
            stack[state.stackTop + 1] = stack[state.stackTop - 1];
            sDbl[state.stackTop + 1] = sDbl[state.stackTop - 1];
            stack[state.stackTop + 2] = stack[state.stackTop];
            sDbl[state.stackTop + 2] = sDbl[state.stackTop];
            state.stackTop += 2;
            return null;
        }
    }

    private static class DoSwap extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            final Object[] stack = frame.stack;
            final double[] sDbl = frame.sDbl;
            Object o = stack[state.stackTop];
            stack[state.stackTop] = stack[state.stackTop - 1];
            stack[state.stackTop - 1] = o;
            double d = sDbl[state.stackTop];
            sDbl[state.stackTop] = sDbl[state.stackTop - 1];
            sDbl[state.stackTop - 1] = d;
            return null;
        }
    }

    private static class DoReturn extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            final Object[] stack = frame.stack;
            final double[] sDbl = frame.sDbl;
            frame.result = stack[state.stackTop];
            frame.resultDbl = sDbl[state.stackTop];
            --state.stackTop;
            return BREAK_LOOP;
        }
    }

    private static class DoReturnResult extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            return BREAK_LOOP;
        }
    }

    private static class DoReturnUndef extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            frame.result = undefined;
            return BREAK_LOOP;
        }
    }

    private static class DoBitNot extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            Object[] stack = frame.stack;
            double[] sDbl = frame.sDbl;
            Number value = stack_numeric(frame, state.stackTop);
            Number result = ScriptRuntime.bitwiseNOT(value);
            if (result instanceof BigInteger) {
                stack[state.stackTop] = result;
            } else {
                stack[state.stackTop] = DOUBLE_MARK;
                sDbl[state.stackTop] = result.doubleValue();
            }
            return null;
        }
    }

    private static class DoBitOp extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            Object[] stack = frame.stack;
            double[] sDbl = frame.sDbl;
            if (stack[state.stackTop] == DOUBLE_MARK && stack[state.stackTop - 1] == DOUBLE_MARK) {
                doFastBitOp(cx, frame, state, op);
                return null;
            }
            Number lValue = stack_numeric(frame, state.stackTop - 1);
            Number rValue = stack_numeric(frame, state.stackTop);
            state.stackTop--;

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
                stack[state.stackTop] = result;
            } else {
                stack[state.stackTop] = DOUBLE_MARK;
                sDbl[state.stackTop] = result.doubleValue();
            }
            return null;
        }

        private static NewState doFastBitOp(
                Context cx, CallFrame frame, InterpreterState state, int op) {
            Object[] stack = frame.stack;
            double[] sDbl = frame.sDbl;
            double lValue = sDbl[state.stackTop - 1];
            double rValue = sDbl[state.stackTop];
            state.stackTop--;

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

            stack[state.stackTop] = DOUBLE_MARK;
            sDbl[state.stackTop] = result;
            return null;
        }
    }

    private static class DoUnsignedRightShift extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            final Object[] stack = frame.stack;
            final double[] sDbl = frame.sDbl;
            double lDbl = stack_double(frame, state.stackTop - 1);
            int rIntValue = stack_int32(frame, state.stackTop) & 0x1F;
            stack[--state.stackTop] = DOUBLE_MARK;
            sDbl[state.stackTop] = ScriptRuntime.toUint32(lDbl) >>> rIntValue;
            return null;
        }
    }

    private static class DoPositive extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            final Object[] stack = frame.stack;
            final double[] sDbl = frame.sDbl;
            double rDbl = stack_double(frame, state.stackTop);
            stack[state.stackTop] = DOUBLE_MARK;
            sDbl[state.stackTop] = rDbl;
            return null;
        }
    }

    private static class DoNegative extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            final Object[] stack = frame.stack;
            final double[] sDbl = frame.sDbl;
            Number rNum = stack_numeric(frame, state.stackTop);
            Number rNegNum = ScriptRuntime.negate(rNum);
            if (rNegNum instanceof BigInteger) {
                stack[state.stackTop] = rNegNum;
            } else {
                stack[state.stackTop] = DOUBLE_MARK;
                sDbl[state.stackTop] = rNegNum.doubleValue();
            }
            return null;
        }
    }

    private static class DoAdd extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            Object rhs = frame.stack[state.stackTop];
            Object lhs = frame.stack[--state.stackTop];
            double d;
            boolean leftRightOrder;
            if (rhs == DOUBLE_MARK) {
                d = frame.sDbl[state.stackTop + 1];
                if (lhs == DOUBLE_MARK) {
                    frame.sDbl[state.stackTop] += d;
                    return null;
                }
                leftRightOrder = true;
                // fallthrough to object + number code
            } else if (lhs == DOUBLE_MARK) {
                d = frame.sDbl[state.stackTop];
                lhs = rhs;
                leftRightOrder = false;
                // fallthrough to object + number code
            } else {
                if (lhs instanceof Scriptable || rhs instanceof Scriptable) {
                    frame.stack[state.stackTop] = ScriptRuntime.add(lhs, rhs, cx);

                    // the next two else if branches are a bit more tricky
                    // to reduce method calls
                } else if (lhs instanceof CharSequence) {
                    if (rhs instanceof CharSequence) {
                        frame.stack[state.stackTop] =
                                new ConsString((CharSequence) lhs, (CharSequence) rhs);
                    } else {
                        frame.stack[state.stackTop] =
                                new ConsString(
                                        (CharSequence) lhs, ScriptRuntime.toCharSequence(rhs));
                    }
                } else if (rhs instanceof CharSequence) {
                    frame.stack[state.stackTop] =
                            new ConsString(ScriptRuntime.toCharSequence(lhs), (CharSequence) rhs);

                } else {
                    Number lNum =
                            (lhs instanceof Number) ? (Number) lhs : ScriptRuntime.toNumeric(lhs);
                    Number rNum =
                            (rhs instanceof Number) ? (Number) rhs : ScriptRuntime.toNumeric(rhs);

                    if (lNum instanceof BigInteger && rNum instanceof BigInteger) {
                        frame.stack[state.stackTop] = ((BigInteger) lNum).add((BigInteger) rNum);
                    } else if (lNum instanceof BigInteger || rNum instanceof BigInteger) {
                        throw ScriptRuntime.typeErrorById("msg.cant.convert.to.number", "BigInt");
                    } else {
                        frame.stack[state.stackTop] = DOUBLE_MARK;
                        frame.sDbl[state.stackTop] = lNum.doubleValue() + rNum.doubleValue();
                    }
                }
                return null;
            }

            // handle object(lhs) + number(d) code
            if (lhs instanceof Scriptable) {
                rhs = ScriptRuntime.wrapNumber(d);
                if (!leftRightOrder) {
                    Object tmp = lhs;
                    lhs = rhs;
                    rhs = tmp;
                }
                frame.stack[state.stackTop] = ScriptRuntime.add(lhs, rhs, cx);
            } else if (lhs instanceof CharSequence) {
                CharSequence rstr = ScriptRuntime.numberToString(d, 10);
                if (leftRightOrder) {
                    frame.stack[state.stackTop] = new ConsString((CharSequence) lhs, rstr);
                } else {
                    frame.stack[state.stackTop] = new ConsString(rstr, (CharSequence) lhs);
                }
            } else {
                Number lNum = (lhs instanceof Number) ? (Number) lhs : ScriptRuntime.toNumeric(lhs);
                if (lNum instanceof BigInteger) {
                    throw ScriptRuntime.typeErrorById("msg.cant.convert.to.number", "BigInt");
                } else {
                    frame.stack[state.stackTop] = DOUBLE_MARK;
                    frame.sDbl[state.stackTop] = lNum.doubleValue() + d;
                }
            }
            return null;
        }
    }

    private static class DoArithmetic extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            Object[] stack = frame.stack;
            double[] sDbl = frame.sDbl;
            if (stack[state.stackTop] == DOUBLE_MARK && stack[state.stackTop - 1] == DOUBLE_MARK) {
                doFastArithemtic(cx, frame, state, op);
                return null;
            }

            Number lNum = stack_numeric(frame, state.stackTop - 1);
            Number rNum = stack_numeric(frame, state.stackTop);
            --state.stackTop;

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
                stack[state.stackTop] = result;
            } else {
                stack[state.stackTop] = DOUBLE_MARK;
                sDbl[state.stackTop] = result.doubleValue();
            }
            return null;
        }

        private static NewState doFastArithemtic(
                Context cx, CallFrame frame, InterpreterState state, int op) {
            Object[] stack = frame.stack;
            double[] sDbl = frame.sDbl;
            double lNum = sDbl[state.stackTop - 1];
            double rNum = sDbl[state.stackTop];
            state.stackTop--;

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

            stack[state.stackTop] = DOUBLE_MARK;
            sDbl[state.stackTop] = result;
            return null;
        }
    }

    private static class DoNot extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            final Object[] stack = frame.stack;
            stack[state.stackTop] = !stack_boolean(frame, state.stackTop);
            return null;
        }
    }

    private static class DoBindName extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            final Object[] stack = frame.stack;
            stack[++state.stackTop] = ScriptRuntime.bind(cx, frame.scope, state.stringReg);
            return null;
        }
    }

    private static class DoSetName extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            final Object[] stack = frame.stack;
            final double[] sDbl = frame.sDbl;
            Object rhs = stack[state.stackTop];
            if (rhs == DOUBLE_MARK) rhs = ScriptRuntime.wrapNumber(sDbl[state.stackTop]);
            Scriptable lhs = (Scriptable) stack[state.stackTop - 1];
            stack[state.stackTop - 1] =
                    op == Token.SETNAME
                            ? ScriptRuntime.setName(lhs, rhs, cx, frame.scope, state.stringReg)
                            : ScriptRuntime.strictSetName(
                                    lhs, rhs, cx, frame.scope, state.stringReg);
            --state.stackTop;
            return null;
        }
    }

    private static class DoStringConcat extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            Object rhs = frame.stack[state.stackTop];
            Object lhs = frame.stack[state.stackTop - 1];

            if (rhs == DOUBLE_MARK) {
                rhs = ScriptRuntime.wrapNumber(frame.sDbl[state.stackTop]);
            }
            if (lhs == DOUBLE_MARK) {
                lhs = ScriptRuntime.wrapNumber(frame.sDbl[state.stackTop - 1]);
            }

            frame.stack[state.stackTop - 1] = ScriptRuntime.concat(lhs, rhs);
            --state.stackTop;
            return null;
        }
    }

    private static class DoSetConst extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            final Object[] stack = frame.stack;
            final double[] sDbl = frame.sDbl;
            Object rhs = stack[state.stackTop];
            if (rhs == DOUBLE_MARK) rhs = ScriptRuntime.wrapNumber(sDbl[state.stackTop]);
            Scriptable lhs = (Scriptable) stack[state.stackTop - 1];
            stack[state.stackTop - 1] = ScriptRuntime.setConst(lhs, rhs, cx, state.stringReg);
            --state.stackTop;
            return null;
        }
    }

    private static class DoDelName extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            Object rhs = frame.stack[state.stackTop];
            if (rhs == DOUBLE_MARK) rhs = ScriptRuntime.wrapNumber(frame.sDbl[state.stackTop]);
            --state.stackTop;
            Object lhs = frame.stack[state.stackTop];
            if (lhs == DOUBLE_MARK) lhs = ScriptRuntime.wrapNumber(frame.sDbl[state.stackTop]);
            frame.stack[state.stackTop] =
                    ScriptRuntime.delete(lhs, rhs, cx, frame.scope, op == Icode_DELNAME);
            return null;
        }
    }

    private static class DoDelPropSuper extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            final Object[] stack = frame.stack;
            state.stackTop -= 1;
            stack[state.stackTop] = Boolean.FALSE;
            ScriptRuntime.throwDeleteOnSuperPropertyNotAllowed();
            return null;
        }
    }

    private static class DoGetPropNoWarn extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            final Object[] stack = frame.stack;
            final double[] sDbl = frame.sDbl;
            Object lhs = stack[state.stackTop];
            if (lhs == DOUBLE_MARK) lhs = ScriptRuntime.wrapNumber(sDbl[state.stackTop]);
            stack[state.stackTop] =
                    ScriptRuntime.getObjectPropNoWarn(lhs, state.stringReg, cx, frame.scope);
            return null;
        }
    }

    private static class DoGetProp extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            final Object[] stack = frame.stack;
            final double[] sDbl = frame.sDbl;
            Object lhs = stack[state.stackTop];
            if (lhs == DOUBLE_MARK) lhs = ScriptRuntime.wrapNumber(sDbl[state.stackTop]);
            stack[state.stackTop] =
                    ScriptRuntime.getObjectProp(lhs, state.stringReg, cx, frame.scope);
            return null;
        }
    }

    private static class DoGetPropSuper extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            final Object[] stack = frame.stack;
            Object superObject = stack[state.stackTop];
            if (superObject == DOUBLE_MARK) Kit.codeBug();
            stack[state.stackTop] =
                    ScriptRuntime.getSuperProp(
                            superObject,
                            state.stringReg,
                            cx,
                            frame.scope,
                            frame.thisObj,
                            op == Token.GETPROPNOWARN_SUPER);
            return null;
        }
    }

    private static class DoSetProp extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            final Object[] stack = frame.stack;
            final double[] sDbl = frame.sDbl;
            Object rhs = stack[state.stackTop];
            if (rhs == DOUBLE_MARK) rhs = ScriptRuntime.wrapNumber(sDbl[state.stackTop]);
            Object lhs = stack[state.stackTop - 1];
            if (lhs == DOUBLE_MARK) lhs = ScriptRuntime.wrapNumber(sDbl[state.stackTop - 1]);
            stack[--state.stackTop] =
                    ScriptRuntime.setObjectProp(lhs, state.stringReg, rhs, cx, frame.scope);
            return null;
        }
    }

    private static class DoSetPropSuper extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            final Object[] stack = frame.stack;
            final double[] sDbl = frame.sDbl;
            Object rhs = stack[state.stackTop];
            if (rhs == DOUBLE_MARK) rhs = ScriptRuntime.wrapNumber(sDbl[state.stackTop]);
            Object superObject = stack[state.stackTop - 1];
            if (superObject == DOUBLE_MARK) Kit.codeBug();
            stack[--state.stackTop] =
                    ScriptRuntime.setSuperProp(
                            superObject, state.stringReg, rhs, cx, frame.scope, frame.thisObj);
            return null;
        }
    }

    private static class DoPropIncDec extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            final Object[] stack = frame.stack;
            final double[] sDbl = frame.sDbl;
            final byte[] iCode = frame.idata.itsICode;
            Object lhs = stack[state.stackTop];
            if (lhs == DOUBLE_MARK) lhs = ScriptRuntime.wrapNumber(sDbl[state.stackTop]);
            stack[state.stackTop] =
                    ScriptRuntime.propIncrDecr(
                            lhs, state.stringReg, cx, frame.scope, iCode[frame.pc]);
            ++frame.pc;
            return null;
        }
    }

    private static class DoGetElem extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            final Object[] stack = frame.stack;
            final double[] sDbl = frame.sDbl;
            Object lhs = stack[--state.stackTop];
            if (lhs == DOUBLE_MARK) {
                lhs = ScriptRuntime.wrapNumber(sDbl[state.stackTop]);
            }
            Object value;
            Object id = stack[state.stackTop + 1];
            if (id != DOUBLE_MARK) {
                value = ScriptRuntime.getObjectElem(lhs, id, cx, frame.scope);
            } else {
                double d = sDbl[state.stackTop + 1];
                value = ScriptRuntime.getObjectIndex(lhs, d, cx, frame.scope);
            }
            stack[state.stackTop] = value;
            return null;
        }
    }

    private static class DoGetElemSuper extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            final Object[] stack = frame.stack;
            final double[] sDbl = frame.sDbl;
            Object superObject = stack[--state.stackTop];
            if (superObject == DOUBLE_MARK) Kit.codeBug();
            Object value;
            Object id = stack[state.stackTop + 1];
            if (id != DOUBLE_MARK) {
                value = ScriptRuntime.getSuperElem(superObject, id, cx, frame.scope, frame.thisObj);
            } else {
                double d = sDbl[state.stackTop + 1];
                value = ScriptRuntime.getSuperIndex(superObject, d, cx, frame.scope, frame.thisObj);
            }
            stack[state.stackTop] = value;
            return null;
        }
    }

    private static class DoSetElem extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            final Object[] stack = frame.stack;
            final double[] sDbl = frame.sDbl;
            Object rhs = stack[state.stackTop];
            if (rhs == DOUBLE_MARK) {
                rhs = ScriptRuntime.wrapNumber(sDbl[state.stackTop]);
            }
            state.stackTop -= 2;
            Object lhs = stack[state.stackTop];
            if (lhs == DOUBLE_MARK) {
                lhs = ScriptRuntime.wrapNumber(sDbl[state.stackTop]);
            }
            Object value;
            Object id = stack[state.stackTop + 1];
            if (id != DOUBLE_MARK) {
                value = ScriptRuntime.setObjectElem(lhs, id, rhs, cx, frame.scope);
            } else {
                double d = sDbl[state.stackTop + 1];
                value = ScriptRuntime.setObjectIndex(lhs, d, rhs, cx, frame.scope);
            }
            stack[state.stackTop] = value;
            return null;
        }
    }

    private static class DoSetElemSuper extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            final Object[] stack = frame.stack;
            final double[] sDbl = frame.sDbl;
            Object rhs = stack[state.stackTop];
            if (rhs == DOUBLE_MARK) {
                rhs = ScriptRuntime.wrapNumber(sDbl[state.stackTop]);
            }
            state.stackTop -= 2;
            Object superObject = stack[state.stackTop];
            if (superObject == DOUBLE_MARK) Kit.codeBug();
            Object value;
            Object id = stack[state.stackTop + 1];
            if (id != DOUBLE_MARK) {
                value =
                        ScriptRuntime.setSuperElem(
                                superObject, id, rhs, cx, frame.scope, frame.thisObj);
            } else {
                double d = sDbl[state.stackTop + 1];
                value =
                        ScriptRuntime.setSuperIndex(
                                superObject, d, rhs, cx, frame.scope, frame.thisObj);
            }
            stack[state.stackTop] = value;
            return null;
        }
    }

    private static class DoElemIncDec extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            final Object[] stack = frame.stack;
            final double[] sDbl = frame.sDbl;
            final byte[] iCode = frame.idata.itsICode;
            Object rhs = stack[state.stackTop];
            if (rhs == DOUBLE_MARK) rhs = ScriptRuntime.wrapNumber(sDbl[state.stackTop]);
            --state.stackTop;
            Object lhs = stack[state.stackTop];
            if (lhs == DOUBLE_MARK) lhs = ScriptRuntime.wrapNumber(sDbl[state.stackTop]);
            stack[state.stackTop] =
                    ScriptRuntime.elemIncrDecr(lhs, rhs, cx, frame.scope, iCode[frame.pc]);
            ++frame.pc;
            return null;
        }
    }

    private static class DoGetRef extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            final Object[] stack = frame.stack;
            Ref ref = (Ref) stack[state.stackTop];
            stack[state.stackTop] = ScriptRuntime.refGet(ref, cx);
            return null;
        }
    }

    private static class DoSetRef extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            final Object[] stack = frame.stack;
            final double[] sDbl = frame.sDbl;
            Object value = stack[state.stackTop];
            if (value == DOUBLE_MARK) value = ScriptRuntime.wrapNumber(sDbl[state.stackTop]);
            Ref ref = (Ref) stack[state.stackTop - 1];
            stack[--state.stackTop] = ScriptRuntime.refSet(ref, value, cx, frame.scope);
            return null;
        }
    }

    private static class DoDelRef extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            final Object[] stack = frame.stack;
            Ref ref = (Ref) stack[state.stackTop];
            stack[state.stackTop] = ScriptRuntime.refDel(ref, cx);
            return null;
        }
    }

    private static class DoRefIncDec extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            final Object[] stack = frame.stack;
            final byte[] iCode = frame.idata.itsICode;
            Ref ref = (Ref) stack[state.stackTop];
            stack[state.stackTop] =
                    ScriptRuntime.refIncrDecr(ref, cx, frame.scope, iCode[frame.pc]);
            ++frame.pc;
            return null;
        }
    }

    private static class DoLocalLoad extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            final Object[] stack = frame.stack;
            final double[] sDbl = frame.sDbl;
            final InterpreterData iData = frame.idata;
            ++state.stackTop;
            state.indexReg += iData.itsMaxVars;
            stack[state.stackTop] = stack[state.indexReg];
            sDbl[state.stackTop] = sDbl[state.indexReg];
            return null;
        }
    }

    private static class DoLocalClear extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            final Object[] stack = frame.stack;
            final InterpreterData iData = frame.idata;
            state.indexReg += iData.itsMaxVars;
            stack[state.indexReg] = null;
            return null;
        }
    }

    private static class DoNameAndThis extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            final Object[] stack = frame.stack;
            // stringReg: name
            stack[++state.stackTop] =
                    ScriptRuntime.getNameAndThis(state.stringReg, cx, frame.scope);
            return null;
        }
    }

    private static class DoNameAndThisOptional extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            final Object[] stack = frame.stack;
            stack[++state.stackTop] =
                    ScriptRuntime.getNameAndThisOptional(state.stringReg, cx, frame.scope);
            return null;
        }
    }

    private static class DoPropAndThis extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            final Object[] stack = frame.stack;
            final double[] sDbl = frame.sDbl;
            Object obj = stack[state.stackTop];
            if (obj == DOUBLE_MARK) obj = ScriptRuntime.wrapNumber(sDbl[state.stackTop]);
            // stringReg: property
            stack[state.stackTop] =
                    ScriptRuntime.getPropAndThis(obj, state.stringReg, cx, frame.scope);
            return null;
        }
    }

    private static class DoPropAndThisOptional extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            final Object[] stack = frame.stack;
            final double[] sDbl = frame.sDbl;
            Object obj = stack[state.stackTop];
            if (obj == DOUBLE_MARK) obj = ScriptRuntime.wrapNumber(sDbl[state.stackTop]);
            // stringReg: property
            stack[state.stackTop] =
                    ScriptRuntime.getPropAndThisOptional(obj, state.stringReg, cx, frame.scope);
            return null;
        }
    }

    private static class DoElemAndThis extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            final Object[] stack = frame.stack;
            final double[] sDbl = frame.sDbl;
            Object obj = stack[state.stackTop - 1];
            if (obj == DOUBLE_MARK) obj = ScriptRuntime.wrapNumber(sDbl[state.stackTop - 1]);
            Object id = stack[state.stackTop];
            if (id == DOUBLE_MARK) id = ScriptRuntime.wrapNumber(sDbl[state.stackTop]);
            stack[--state.stackTop] = ScriptRuntime.getElemAndThis(obj, id, cx, frame.scope);
            return null;
        }
    }

    private static class DoElemAndThisOptional extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            final Object[] stack = frame.stack;
            final double[] sDbl = frame.sDbl;
            Object obj = stack[state.stackTop - 1];
            if (obj == DOUBLE_MARK) obj = ScriptRuntime.wrapNumber(sDbl[state.stackTop - 1]);
            Object id = stack[state.stackTop];
            if (id == DOUBLE_MARK) id = ScriptRuntime.wrapNumber(sDbl[state.stackTop]);
            stack[--state.stackTop] =
                    ScriptRuntime.getElemAndThisOptional(obj, id, cx, frame.scope);
            return null;
        }
    }

    private static class DoValueAndThis extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            final Object[] stack = frame.stack;
            final double[] sDbl = frame.sDbl;
            Object value = stack[state.stackTop];
            if (value == DOUBLE_MARK) value = ScriptRuntime.wrapNumber(sDbl[state.stackTop]);
            stack[state.stackTop] = ScriptRuntime.getValueAndThis(value, cx);
            return null;
        }
    }

    private static class DoValueAndThisOptional extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            Object[] stack = frame.stack;
            double[] sDbl = frame.sDbl;
            Object value = stack[state.stackTop];
            if (value == DOUBLE_MARK) value = ScriptRuntime.wrapNumber(sDbl[state.stackTop]);
            stack[state.stackTop] = ScriptRuntime.getValueAndThisOptional(value, cx);
            return null;
        }
    }

    private static class DoCallSpecial extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            Object[] stack = frame.stack;
            double[] sDbl = frame.sDbl;
            byte[] iCode = frame.idata.itsICode;
            boolean isOptionalChainingCall = (op == Icode_CALLSPECIAL_OPTIONAL);

            if (state.instructionCounting) {
                cx.instructionCount += INVOCATION_COST;
            }
            int callType = iCode[frame.pc] & 0xFF;
            boolean isNew = (iCode[frame.pc + 1] != 0);
            int sourceLine = getIndex(iCode, frame.pc + 2);

            // indexReg: number of arguments
            if (isNew) {
                // stack change: function arg0 .. argN -> newResult
                state.stackTop -= state.indexReg;

                Object function = stack[state.stackTop];
                if (function == DOUBLE_MARK)
                    function = ScriptRuntime.wrapNumber(sDbl[state.stackTop]);
                Object[] outArgs = getArgsArray(stack, sDbl, state.stackTop + 1, state.indexReg);
                stack[state.stackTop] =
                        ScriptRuntime.newSpecial(cx, function, outArgs, frame.scope, callType);
            } else {
                // stack change: function thisObj arg0 .. argN -> result
                state.stackTop -= state.indexReg;

                // Call code generation ensure that stack here
                // is ... Callable Scriptable
                ScriptRuntime.LookupResult result =
                        (ScriptRuntime.LookupResult) stack[state.stackTop];
                Object[] outArgs = getArgsArray(stack, sDbl, state.stackTop + 1, state.indexReg);
                Callable function = result.getCallable();
                stack[state.stackTop] =
                        ScriptRuntime.callSpecial(
                                cx,
                                function,
                                result.getThis(),
                                outArgs,
                                frame.scope,
                                frame.thisObj,
                                callType,
                                frame.fnOrScript.getDescriptor().getSourceName(),
                                sourceLine,
                                isOptionalChainingCall);
            }
            frame.pc += 4;
            return null;
        }
    }

    private static class DoCallByteCode extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {

            Object[] stack = frame.stack;
            double[] sDbl = frame.sDbl;
            Object[] boundArgs = null;
            int blen = 0;

            if (state.instructionCounting) {
                cx.instructionCount += INVOCATION_COST;
            }
            // stack change: lookup_result arg0 .. argN -> result
            // indexReg: number of arguments
            state.stackTop -= state.indexReg;
            ScriptRuntime.LookupResult result = (ScriptRuntime.LookupResult) stack[state.stackTop];
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
                Object[] outArgs = getArgsArray(stack, sDbl, state.stackTop + 1, state.indexReg);
                stack[state.stackTop] =
                        ScriptRuntime.callRef(
                                fun, funThisObj,
                                outArgs, cx);
                return null;
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
                if (fun instanceof KnownBuiltInFunction) {
                    KnownBuiltInFunction kfun = (KnownBuiltInFunction) fun;
                    // Bug 405654 -- make the best effort to keep
                    // Function.apply and Function.call within this
                    // interpreter loop invocation
                    if (BaseFunction.isApplyOrCall(kfun)) {
                        // funThisObj becomes fun
                        fun = ScriptRuntime.getCallable(funThisObj);
                        // first arg becomes thisObj
                        funThisObj =
                                getApplyThis(
                                        cx,
                                        stack,
                                        sDbl,
                                        boundArgs,
                                        state.stackTop + 1,
                                        state.indexReg,
                                        fun,
                                        frame);
                        if (BaseFunction.isApply(kfun)) {
                            // Apply: second argument after new "this"
                            // should be array-like
                            // and we'll spread its elements on the stack
                            final Object[] callArgs;
                            if (blen > 1) {
                                callArgs = ScriptRuntime.getApplyArguments(cx, boundArgs[1]);
                            } else if (state.indexReg < 2) {
                                callArgs = ScriptRuntime.emptyArgs;
                            } else {
                                callArgs =
                                        ScriptRuntime.getApplyArguments(
                                                cx, stack[state.stackTop - blen + 2]);
                            }

                            int alen = callArgs.length;
                            // We're coming from the outside, so this
                            // is replacing any bound args we might
                            // have had already.
                            boundArgs = callArgs;
                            blen = alen;
                            state.indexReg = alen;
                        } else {
                            // Call: shift args left, starting from 2nd
                            if (state.indexReg > 0) {
                                if (state.indexReg > 1 && blen == 0) {
                                    System.arraycopy(
                                            stack,
                                            state.stackTop + 2,
                                            stack,
                                            state.stackTop + 1,
                                            state.indexReg - 1);
                                    System.arraycopy(
                                            sDbl,
                                            state.stackTop + 2,
                                            sDbl,
                                            state.stackTop + 1,
                                            state.indexReg - 1);
                                } else if (state.indexReg > 1) {
                                    Object[] newBArgs = new Object[boundArgs.length - 1];
                                    System.arraycopy(
                                            boundArgs, 1, newBArgs, 0, boundArgs.length - 1);
                                    boundArgs = newBArgs;
                                    blen = newBArgs.length;
                                } else {
                                    // Bound args is 1 long.
                                    boundArgs = new Object[0];
                                    blen = 0;
                                }
                                state.indexReg--;
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
                    boundArgs = addBoundArgs(boundArgs, bArgs);
                    blen = blen + bArgs.length;
                    state.indexReg += bArgs.length;
                } else if (fun instanceof NoSuchMethodShim) {
                    NoSuchMethodShim nsmfun = (NoSuchMethodShim) fun;
                    // Bug 447697 -- make best effort to keep
                    // __noSuchMethod__ within this interpreter loop
                    // invocation.
                    Object[] elements =
                            getArgsArray(
                                    stack,
                                    sDbl,
                                    boundArgs,
                                    blen,
                                    state.stackTop + 1,
                                    state.indexReg);
                    fun = nsmfun.noSuchMethodMethod;
                    boundArgs = new Object[2];
                    blen = 2;
                    boundArgs[0] = nsmfun.methodName;
                    boundArgs[1] = cx.newArray(calleeScope, elements);
                    state.indexReg = 2;
                } else if (fun == null) {
                    throw ScriptRuntime.notFunctionError(null, null);
                } else {
                    // Current function is something that we can't reduce
                    // further.
                    break;
                }
            }

            if (fun instanceof JSFunction
                    && ((JSFunction) fun).getDescriptor().getCode() instanceof InterpreterData) {
                JSFunction ifun = (JSFunction) fun;
                JSDescriptor desc = ifun.getDescriptor();
                InterpreterData idata = (InterpreterData) desc.getCode();
                if (frame.fnOrScript.getDescriptor().getSecurityDomain()
                        == desc.getSecurityDomain()) {
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
                                    ifun.getFunctionThis(funThisObj),
                                    funHomeObj,
                                    stack,
                                    sDbl,
                                    boundArgs,
                                    state.stackTop + 1,
                                    state.indexReg,
                                    ifun,
                                    idata,
                                    callParentFrame);
                    if (op != Icode_TAIL_CALL) {
                        frame.savedStackTop = state.stackTop;
                        frame.savedCallOp = op;
                    }
                    return new StateContinueResult(calleeFrame, state.indexReg);
                }
            }

            if (fun instanceof NativeContinuation) {
                // Jump to the captured continuation
                ContinuationJump cjump;
                cjump = new ContinuationJump((NativeContinuation) fun, frame);

                // continuation result is the first argument if any
                // of continuation call
                if (state.indexReg == 0) {
                    cjump.result = undefined;
                } else {
                    cjump.result = stack[state.stackTop + 1];
                    cjump.resultDbl = sDbl[state.stackTop + 1];
                }

                // Start the real unwind job
                state.throwable = cjump;
                return BREAK_WITHOUT_EXTENSION;
            }

            if (fun instanceof IdFunctionObject) {
                IdFunctionObject ifun = (IdFunctionObject) fun;
                if (NativeContinuation.isContinuationConstructor(ifun)) {
                    frame.stack[state.stackTop] = captureContinuation(cx, frame.parentFrame, false);
                    return null;
                }
            }

            frame.savedCallOp = op;
            frame.savedStackTop = state.stackTop;
            stack[state.stackTop] =
                    fun.call(
                            cx,
                            calleeScope,
                            funThisObj,
                            getArgsArray(
                                    stack,
                                    sDbl,
                                    boundArgs,
                                    blen,
                                    state.stackTop + 1,
                                    state.indexReg));

            return null;
        }
    }

    private static class DoNew extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            if (state.instructionCounting) {
                cx.instructionCount += INVOCATION_COST;
            }
            // stack change: function arg0 .. argN -> newResult
            // state.indexReg: number of arguments
            state.stackTop -= state.indexReg;

            Object lhs = frame.stack[state.stackTop];
            if (lhs instanceof JSFunction
                    && ((JSFunction) lhs).getConstructor() instanceof InterpreterData) {
                JSFunction f = (JSFunction) lhs;
                JSDescriptor desc = f.getDescriptor();
                InterpreterData idata = (InterpreterData) desc.getConstructor();
                if (frame.fnOrScript.getDescriptor().getSecurityDomain()
                        == desc.getSecurityDomain()) {
                    if (cx.getLanguageVersion() >= Context.VERSION_ES6
                            && f.getHomeObject() != null) {
                        // Only methods have home objects associated with
                        // them
                        throw ScriptRuntime.typeErrorById("msg.not.ctor", f.getFunctionName());
                    }

                    Scriptable newInstance =
                            f.getHomeObject() == null ? f.createObject(cx, frame.scope) : null;
                    CallFrame calleeFrame =
                            initFrame(
                                    cx,
                                    frame.scope,
                                    newInstance,
                                    newInstance,
                                    frame.stack,
                                    frame.sDbl,
                                    null,
                                    state.stackTop + 1,
                                    state.indexReg,
                                    f,
                                    idata,
                                    frame);

                    frame.stack[state.stackTop] = newInstance;
                    frame.savedStackTop = state.stackTop;
                    frame.savedCallOp = op;
                    return new StateContinueResult(calleeFrame, state.indexReg);
                }
            }
            if (!(lhs instanceof Constructable)) {
                if (lhs == DOUBLE_MARK) lhs = ScriptRuntime.wrapNumber(frame.sDbl[state.stackTop]);
                throw ScriptRuntime.notFunctionError(lhs);
            }
            Constructable ctor = (Constructable) lhs;

            if (ctor instanceof IdFunctionObject) {
                IdFunctionObject ifun = (IdFunctionObject) ctor;
                if (NativeContinuation.isContinuationConstructor(ifun)) {
                    frame.stack[state.stackTop] = captureContinuation(cx, frame.parentFrame, false);
                    return null;
                }
            }

            Object[] outArgs =
                    getArgsArray(frame.stack, frame.sDbl, state.stackTop + 1, state.indexReg);
            frame.stack[state.stackTop] = ctor.construct(cx, frame.scope, outArgs);
            return null;
        }
    }

    private static class DoTypeOf extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            Object[] stack = frame.stack;
            double[] sDbl = frame.sDbl;
            Object lhs = stack[state.stackTop];
            if (lhs == DOUBLE_MARK) lhs = ScriptRuntime.wrapNumber(sDbl[state.stackTop]);
            stack[state.stackTop] = ScriptRuntime.typeof(lhs);
            return null;
        }
    }

    private static class DoTypeOfName extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            frame.stack[++state.stackTop] = ScriptRuntime.typeofName(frame.scope, state.stringReg);
            return null;
        }
    }

    private static class DoString extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            frame.stack[++state.stackTop] = state.stringReg;
            return null;
        }
    }

    private static class DoShortNumber extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            ++state.stackTop;
            frame.stack[state.stackTop] = DOUBLE_MARK;
            frame.sDbl[state.stackTop] = getShort(frame.idata.itsICode, frame.pc);
            frame.pc += 2;
            return null;
        }
    }

    private static class DoIntNumber extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            ++state.stackTop;
            frame.stack[state.stackTop] = DOUBLE_MARK;
            frame.sDbl[state.stackTop] = getInt(frame.idata.itsICode, frame.pc);
            frame.pc += 4;
            return null;
        }
    }

    private static class DoNumber extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            ++state.stackTop;
            frame.stack[state.stackTop] = DOUBLE_MARK;
            frame.sDbl[state.stackTop] = frame.idata.itsDoubleTable[state.indexReg];
            return null;
        }
    }

    private static class DoBigInt extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            frame.stack[++state.stackTop] = state.bigIntReg;
            return null;
        }
    }

    private static class DoName extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            frame.stack[++state.stackTop] = ScriptRuntime.name(cx, frame.scope, state.stringReg);
            return null;
        }
    }

    private static class DoNameIncDec extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            frame.stack[++state.stackTop] =
                    ScriptRuntime.nameIncrDecr(
                            frame.scope, state.stringReg, cx, frame.idata.itsICode[frame.pc]);
            ++frame.pc;
            return null;
        }
    }

    private static class DoSetConstVar1 extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            state.indexReg = frame.idata.itsICode[frame.pc++];
            var varAttributes = frame.varSource.stackAttributes;
            var vars = frame.varSource.stack;
            var varDbls = frame.varSource.sDbl;
            if (!frame.useActivation) {
                if ((varAttributes[state.indexReg] & ScriptableObject.READONLY) == 0) {
                    throw Context.reportRuntimeErrorById(
                            "msg.var.redecl",
                            frame.fnOrScript.getDescriptor().getParamOrVarName(state.indexReg));
                }
                if ((varAttributes[state.indexReg] & ScriptableObject.UNINITIALIZED_CONST) != 0) {
                    vars[state.indexReg] = frame.stack[state.stackTop];
                    varAttributes[state.indexReg] &= ~ScriptableObject.UNINITIALIZED_CONST;
                    varDbls[state.indexReg] = frame.sDbl[state.stackTop];
                }
            } else {
                Object val = frame.stack[state.stackTop];
                if (val == DOUBLE_MARK) val = ScriptRuntime.wrapNumber(frame.sDbl[state.stackTop]);
                String stringReg =
                        frame.fnOrScript.getDescriptor().getParamOrVarName(state.indexReg);
                if (frame.scope instanceof ConstProperties) {
                    ConstProperties cp = (ConstProperties) frame.scope;
                    cp.putConst(stringReg, frame.scope, val);
                } else throw Kit.codeBug();
            }
            return null;
        }
    }

    private static class DoSetConstVar extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            var varAttributes = frame.varSource.stackAttributes;
            var vars = frame.varSource.stack;
            var varDbls = frame.varSource.sDbl;
            if (!frame.useActivation) {
                if ((varAttributes[state.indexReg] & ScriptableObject.READONLY) == 0) {
                    throw Context.reportRuntimeErrorById(
                            "msg.var.redecl",
                            frame.fnOrScript.getDescriptor().getParamOrVarName(state.indexReg));
                }
                if ((varAttributes[state.indexReg] & ScriptableObject.UNINITIALIZED_CONST) != 0) {
                    vars[state.indexReg] = frame.stack[state.stackTop];
                    varAttributes[state.indexReg] &= ~ScriptableObject.UNINITIALIZED_CONST;
                    varDbls[state.indexReg] = frame.sDbl[state.stackTop];
                }
            } else {
                Object val = frame.stack[state.stackTop];
                if (val == DOUBLE_MARK) val = ScriptRuntime.wrapNumber(frame.sDbl[state.stackTop]);
                String stringReg =
                        frame.fnOrScript.getDescriptor().getParamOrVarName(state.indexReg);
                if (frame.scope instanceof ConstProperties) {
                    ConstProperties cp = (ConstProperties) frame.scope;
                    cp.putConst(stringReg, frame.scope, val);
                } else throw Kit.codeBug();
            }
            return null;
        }
    }

    private static class DoSetVar1 extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            state.indexReg = frame.idata.itsICode[frame.pc++];
            var varAttributes = frame.varSource.stackAttributes;
            var vars = frame.varSource.stack;
            var varDbls = frame.varSource.sDbl;
            if (!frame.useActivation) {
                if ((varAttributes[state.indexReg] & ScriptableObject.READONLY) == 0) {
                    vars[state.indexReg] = frame.stack[state.stackTop];
                    varDbls[state.indexReg] = frame.sDbl[state.stackTop];
                }
            } else {
                Object val = frame.stack[state.stackTop];
                if (val == DOUBLE_MARK) val = ScriptRuntime.wrapNumber(frame.sDbl[state.stackTop]);
                String stringReg =
                        frame.fnOrScript.getDescriptor().getParamOrVarName(state.indexReg);
                frame.scope.put(stringReg, frame.scope, val);
            }
            return null;
        }
    }

    private static class DoSetVar extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            var varAttributes = frame.varSource.stackAttributes;
            var vars = frame.varSource.stack;
            var varDbls = frame.varSource.sDbl;
            if (!frame.useActivation) {
                if ((varAttributes[state.indexReg] & ScriptableObject.READONLY) == 0) {
                    vars[state.indexReg] = frame.stack[state.stackTop];
                    varDbls[state.indexReg] = frame.sDbl[state.stackTop];
                }
            } else {
                Object val = frame.stack[state.stackTop];
                if (val == DOUBLE_MARK) val = ScriptRuntime.wrapNumber(frame.sDbl[state.stackTop]);
                String stringReg =
                        frame.fnOrScript.getDescriptor().getParamOrVarName(state.indexReg);
                frame.scope.put(stringReg, frame.scope, val);
            }
            return null;
        }
    }

    private static class DoGetVar1 extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            state.indexReg = frame.idata.itsICode[frame.pc++];
            var vars = frame.varSource.stack;
            var varDbls = frame.varSource.sDbl;
            ++state.stackTop;
            if (!frame.useActivation) {
                frame.stack[state.stackTop] = vars[state.indexReg];
                frame.sDbl[state.stackTop] = varDbls[state.indexReg];
            } else {
                String stringReg =
                        frame.fnOrScript.getDescriptor().getParamOrVarName(state.indexReg);
                frame.stack[state.stackTop] = frame.scope.get(stringReg, frame.scope);
            }
            return null;
        }
    }

    private static class DoGetVar extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            var vars = frame.varSource.stack;
            var varDbls = frame.varSource.sDbl;
            ++state.stackTop;
            if (!frame.useActivation) {
                frame.stack[state.stackTop] = vars[state.indexReg];
                frame.sDbl[state.stackTop] = varDbls[state.indexReg];
            } else {
                String stringReg =
                        frame.fnOrScript.getDescriptor().getParamOrVarName(state.indexReg);
                frame.stack[state.stackTop] = frame.scope.get(stringReg, frame.scope);
            }
            return null;
        }
    }

    private static class DoVarIncDec extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            var varAttributes = frame.varSource.stackAttributes;
            var vars = frame.varSource.stack;
            var varDbls = frame.varSource.sDbl;
            // indexReg : varindex
            ++state.stackTop;
            int incrDecrMask = frame.idata.itsICode[frame.pc];
            if (!frame.useActivation) {
                Object varValue = vars[state.indexReg];
                double d = 0.0;
                BigInteger bi = null;
                if (varValue == DOUBLE_MARK) {
                    d = varDbls[state.indexReg];
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
                    if ((varAttributes[state.indexReg] & ScriptableObject.READONLY) == 0) {
                        if (varValue != DOUBLE_MARK) {
                            vars[state.indexReg] = DOUBLE_MARK;
                        }
                        varDbls[state.indexReg] = d2;
                        frame.stack[state.stackTop] = DOUBLE_MARK;
                        frame.sDbl[state.stackTop] = post ? d : d2;
                    } else {
                        if (post && varValue != DOUBLE_MARK) {
                            frame.stack[state.stackTop] = varValue;
                        } else {
                            frame.stack[state.stackTop] = DOUBLE_MARK;
                            frame.sDbl[state.stackTop] = post ? d : d2;
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
                    if ((varAttributes[state.indexReg] & ScriptableObject.READONLY) == 0) {
                        vars[state.indexReg] = result;
                        frame.stack[state.stackTop] = post ? bi : result;
                    } else {
                        if (post && varValue != DOUBLE_MARK) {
                            frame.stack[state.stackTop] = varValue;
                        } else {
                            frame.stack[state.stackTop] = post ? bi : result;
                        }
                    }
                }
            } else {
                String varName = frame.fnOrScript.getDescriptor().getParamOrVarName(state.indexReg);
                frame.stack[state.stackTop] =
                        ScriptRuntime.nameIncrDecr(frame.scope, varName, cx, incrDecrMask);
            }
            ++frame.pc;
            return null;
        }
    }

    private static class DoZero extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            ++state.stackTop;
            frame.stack[state.stackTop] = Integer.valueOf(0);
            return null;
        }
    }

    private static class DoOne extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            ++state.stackTop;
            frame.stack[state.stackTop] = Integer.valueOf(1);
            return null;
        }
    }

    private static class DoNull extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            frame.stack[++state.stackTop] = null;
            return null;
        }
    }

    private static class DoThis extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            frame.stack[++state.stackTop] = frame.thisObj;
            return null;
        }
    }

    private static class DoSuper extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            // If we are referring to "super", then we always have an
            // activation
            // (this is done in IrFactory). The home object is stored as
            // part of the
            // activation frame to propagate it correctly for nested
            // functions.
            Scriptable homeObject = frame.fnOrScript.getHomeObject();
            if (homeObject == null) {
                // This if is specified in the spec, but I cannot imagine
                // how the home object will ever be null since `super` is
                // legal _only_ in method definitions, where we do have a
                // home object!
                frame.stack[++state.stackTop] = Undefined.instance;
            } else {
                frame.stack[++state.stackTop] = homeObject.getPrototype();
            }
            return null;
        }
    }

    private static class DoThisFunction extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            frame.stack[++state.stackTop] = frame.fnOrScript;
            return null;
        }
    }

    private static class DoFalse extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            frame.stack[++state.stackTop] = Boolean.FALSE;
            return null;
        }
    }

    private static class DoTrue extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            frame.stack[++state.stackTop] = Boolean.TRUE;
            return null;
        }
    }

    private static class DoUndef extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            frame.stack[++state.stackTop] = undefined;
            return null;
        }
    }

    private static class DoEnterWith extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            Object lhs = frame.stack[state.stackTop];
            if (lhs == DOUBLE_MARK) lhs = ScriptRuntime.wrapNumber(frame.sDbl[state.stackTop]);
            frame.scope = ScriptRuntime.enterWith(lhs, cx, frame.scope);
            state.stackTop--;
            return null;
        }
    }

    private static class DoLeaveWith extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            frame.scope = ScriptRuntime.leaveWith(frame.scope);
            return null;
        }
    }

    private static class DoCatchScope extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {

            // stack top: exception object
            // state.stringReg: name of exception variable
            // state.indexReg: local for exception scope
            --state.stackTop;
            state.indexReg += frame.idata.itsMaxVars;

            boolean afterFirstScope = (frame.idata.itsICode[frame.pc] != 0);
            Throwable caughtException = (Throwable) frame.stack[state.stackTop + 1];
            Scriptable lastCatchScope;
            if (!afterFirstScope) {
                lastCatchScope = null;
            } else {
                lastCatchScope = (Scriptable) frame.stack[state.indexReg];
            }
            frame.stack[state.indexReg] =
                    ScriptRuntime.newCatchScope(
                            caughtException, lastCatchScope, state.stringReg, cx, frame.scope);
            ++frame.pc;
            return null;
        }
    }

    private static class DoEnumInit extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            Object lhs = frame.stack[state.stackTop];
            if (lhs == DOUBLE_MARK) lhs = ScriptRuntime.wrapNumber(frame.sDbl[state.stackTop]);
            state.indexReg += frame.idata.itsMaxVars;
            int enumType =
                    op == Token.ENUM_INIT_KEYS
                            ? ScriptRuntime.ENUMERATE_KEYS
                            : op == Token.ENUM_INIT_VALUES
                                    ? ScriptRuntime.ENUMERATE_VALUES
                                    : op == Token.ENUM_INIT_VALUES_IN_ORDER
                                            ? ScriptRuntime.ENUMERATE_VALUES_IN_ORDER
                                            : ScriptRuntime.ENUMERATE_ARRAY;
            frame.stack[state.indexReg] = ScriptRuntime.enumInit(lhs, cx, frame.scope, enumType);
            --state.stackTop;
            return null;
        }
    }

    private static class DoEnumOp extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            state.indexReg += frame.idata.itsMaxVars;
            Object val = frame.stack[state.indexReg];
            frame.stack[++state.stackTop] =
                    (op == Token.ENUM_NEXT)
                            ? ScriptRuntime.enumNext(val, cx)
                            : ScriptRuntime.enumId(val, cx);
            return null;
        }
    }

    private static class DoRefSpecial extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            // stringReg: name of special property
            Object obj = frame.stack[state.stackTop];
            if (obj == DOUBLE_MARK) obj = ScriptRuntime.wrapNumber(frame.sDbl[state.stackTop]);
            frame.stack[state.stackTop] =
                    ScriptRuntime.specialRef(obj, state.stringReg, cx, frame.scope);
            return null;
        }
    }

    private static class DoRefMember extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            Object elem = frame.stack[state.stackTop];
            if (elem == DOUBLE_MARK) elem = ScriptRuntime.wrapNumber(frame.sDbl[state.stackTop]);
            --state.stackTop;
            Object obj = frame.stack[state.stackTop];
            if (obj == DOUBLE_MARK) obj = ScriptRuntime.wrapNumber(frame.sDbl[state.stackTop]);
            frame.stack[state.stackTop] = ScriptRuntime.memberRef(obj, elem, cx, state.indexReg);
            return null;
        }
    }

    private static class DoRefNsMember extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            Object elem = frame.stack[state.stackTop];
            if (elem == DOUBLE_MARK) elem = ScriptRuntime.wrapNumber(frame.sDbl[state.stackTop]);
            --state.stackTop;
            Object ns = frame.stack[state.stackTop];
            if (ns == DOUBLE_MARK) ns = ScriptRuntime.wrapNumber(frame.sDbl[state.stackTop]);
            --state.stackTop;
            Object obj = frame.stack[state.stackTop];
            if (obj == DOUBLE_MARK) obj = ScriptRuntime.wrapNumber(frame.sDbl[state.stackTop]);
            frame.stack[state.stackTop] =
                    ScriptRuntime.memberRef(obj, ns, elem, cx, state.indexReg);
            return null;
        }
    }

    private static class DoRefName extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            // indexReg: flags
            Object name = frame.stack[state.stackTop];
            if (name == DOUBLE_MARK) name = ScriptRuntime.wrapNumber(frame.sDbl[state.stackTop]);
            frame.stack[state.stackTop] =
                    ScriptRuntime.nameRef(name, cx, frame.scope, state.indexReg);
            return null;
        }
    }

    private static class DoRefNsName extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            Object name = frame.stack[state.stackTop];
            if (name == DOUBLE_MARK) name = ScriptRuntime.wrapNumber(frame.sDbl[state.stackTop]);
            --state.stackTop;
            Object ns = frame.stack[state.stackTop];
            if (ns == DOUBLE_MARK) ns = ScriptRuntime.wrapNumber(frame.sDbl[state.stackTop]);
            frame.stack[state.stackTop] =
                    ScriptRuntime.nameRef(ns, name, cx, frame.scope, state.indexReg);
            return null;
        }
    }

    private static class DoScopeLoad extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            state.indexReg += frame.idata.itsMaxVars;
            frame.scope = (Scriptable) frame.stack[state.indexReg];
            return null;
        }
    }

    private static class DoScopeSave extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            state.indexReg += frame.idata.itsMaxVars;
            frame.stack[state.indexReg] = frame.scope;
            return null;
        }
    }

    private static class DoClosureExpr extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            JSFunction fn = createClosure(cx, frame, state.indexReg);
            frame.stack[++state.stackTop] = fn;
            return null;
        }
    }

    private static class DoMethodExpr extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            Scriptable homeObject = (Scriptable) frame.stack[state.stackTop - 1];
            JSFunction fn = createMethod(cx, frame, state.indexReg, homeObject);
            frame.stack[++state.stackTop] = fn;
            return null;
        }
    }

    private static class DoClosureStatement extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            initFunction(cx, frame.scope, frame.fnOrScript.getDescriptor(), state.indexReg);
            return null;
        }
    }

    private static class DoRegExp extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            Object re = frame.idata.itsRegExpLiterals[state.indexReg];
            frame.stack[++state.stackTop] = ScriptRuntime.wrapRegExp(cx, frame.scope, re);
            return null;
        }
    }

    private static class DoTemplateLiteralCallSite extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            Object[] templateLiterals = frame.idata.itsTemplateLiterals;
            frame.stack[++state.stackTop] =
                    ScriptRuntime.getTemplateLiteralCallSite(
                            cx, frame.scope, templateLiterals, state.indexReg);
            return null;
        }
    }

    private static class DoLiteralNewObject extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            ++frame.pc;
            ++state.stackTop;
            frame.stack[state.stackTop] = cx.newObject(frame.scope);
            ++state.stackTop;

            // indexReg > 0: index of constant with the keys
            // indexReg < 0: we have a spread, so no keys array, but we know the length
            if (state.indexReg < 0) {
                frame.stack[state.stackTop] =
                        NewLiteralStorage.create(cx, -state.indexReg - 1, true);
            } else {
                Object[] ids = (Object[]) frame.idata.literalIds[state.indexReg];
                boolean copyArray = frame.idata.itsICode[frame.pc] != 0;
                frame.stack[state.stackTop] =
                        NewLiteralStorage.create(
                                cx, copyArray ? Arrays.copyOf(ids, ids.length) : ids);
            }

            return null;
        }
    }

    private static class DoLiteralNewArray extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            // indexReg: number of values in the literal
            frame.stack[++state.stackTop] = NewLiteralStorage.create(cx, state.indexReg, false);
            return null;
        }
    }

    private static class DoLiteralSet extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            Object value = frame.stack[state.stackTop];
            if (value == DOUBLE_MARK) value = ScriptRuntime.wrapNumber(frame.sDbl[state.stackTop]);
            --state.stackTop;
            var store = (NewLiteralStorage) frame.stack[state.stackTop];
            store.pushValue(value);
            return null;
        }
    }

    private static class DoLiteralGetter extends InstructionClass {
        @Override
        NewState execute(Context cs, CallFrame frame, InterpreterState state, int op) {
            Object value = frame.stack[state.stackTop];
            --state.stackTop;
            var store = (NewLiteralStorage) frame.stack[state.stackTop];
            store.pushGetter(value);
            return null;
        }
    }

    private static class DoLiteralSetter extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            Object value = frame.stack[state.stackTop];
            --state.stackTop;
            var store = (NewLiteralStorage) frame.stack[state.stackTop];
            store.pushSetter(value);
            return null;
        }
    }

    private static class DoLiteralKeySet extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            Object key = frame.stack[state.stackTop];
            if (key == DOUBLE_MARK) key = ScriptRuntime.wrapNumber(frame.sDbl[state.stackTop]);
            --state.stackTop;
            var store = (NewLiteralStorage) frame.stack[state.stackTop];
            store.pushKey(key);
            return null;
        }
    }

    private static class DoSpread extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            // stack: [..., NewLiteralStorage, sourceObj]
            Object source = frame.stack[state.stackTop];
            --state.stackTop;
            NewLiteralStorage store = (NewLiteralStorage) frame.stack[state.stackTop];
            store.spread(cx, frame.scope, source);
            return null;
        }
    }

    private static class DoObjectLit extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            var store = (NewLiteralStorage) frame.stack[state.stackTop];
            --state.stackTop;
            Scriptable object = (Scriptable) frame.stack[state.stackTop];
            ScriptRuntime.fillObjectLiteral(
                    object,
                    store.getKeys(),
                    store.getValues(),
                    store.getGetterSetters(),
                    cx,
                    frame.scope);
            return null;
        }
    }

    private static class DoArrayLiteral extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            var store = (NewLiteralStorage) frame.stack[state.stackTop];
            int[] skipIndexces = null;
            if (op == Icode_SPARE_ARRAYLIT) {
                skipIndexces = (int[]) frame.idata.literalIds[state.indexReg];
            }
            frame.stack[state.stackTop] =
                    ScriptRuntime.newArrayLiteral(store.getValues(), skipIndexces, cx, frame.scope);
            return null;
        }
    }

    private static class DoEnterDotQuery extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            Object lhs = frame.stack[state.stackTop];
            if (lhs == DOUBLE_MARK) lhs = ScriptRuntime.wrapNumber(frame.sDbl[state.stackTop]);
            frame.scope = ScriptRuntime.enterDotQuery(lhs, frame.scope);
            state.stackTop--;
            return null;
        }
    }

    private static class DoLeaveDotQuery extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            boolean valBln = stack_boolean(frame, state.stackTop);
            Object x = ScriptRuntime.updateDotQuery(valBln, frame.scope);
            if (x != null) {
                frame.stack[state.stackTop] = x;
                frame.scope = ScriptRuntime.leaveDotQuery(frame.scope);
                frame.pc += 2;
                return null;
            }
            // reset stack and PC to code after ENTERDQ
            --state.stackTop;
            return BREAK_JUMPLESSRUN;
        }
    }

    private static class DoDefaultNamespace extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            Object value = frame.stack[state.stackTop];
            if (value == DOUBLE_MARK) value = ScriptRuntime.wrapNumber(frame.sDbl[state.stackTop]);
            frame.stack[state.stackTop] = ScriptRuntime.setDefaultNamespace(value, cx);
            return null;
        }
    }

    private static class DoEscXMLAttr extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            Object value = frame.stack[state.stackTop];
            if (value != DOUBLE_MARK) {
                frame.stack[state.stackTop] = ScriptRuntime.escapeAttributeValue(value, cx);
            }
            return null;
        }
    }

    private static class DoEscXMLText extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            Object value = frame.stack[state.stackTop];
            if (value != DOUBLE_MARK) {
                frame.stack[state.stackTop] = ScriptRuntime.escapeTextValue(value, cx);
            }
            return null;
        }
    }

    private static class DoDebug extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            if (frame.debuggerFrame != null) {
                frame.debuggerFrame.onDebuggerStatement(cx);
            }
            return null;
        }
    }

    private static class DoLineChange extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            frame.pcSourceLineStart = frame.pc;
            if (frame.debuggerFrame != null) {
                int line = getIndex(frame.idata.itsICode, frame.pc);
                frame.debuggerFrame.onLineChange(cx, line);
            }
            frame.pc += 2;
            return null;
        }
    }

    private static class DoIndexCn extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            state.indexReg = Icode_REG_IND_C0 - op;
            return null;
        }
    }

    private static class DoRegIndex1 extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            state.indexReg = 0xFF & frame.idata.itsICode[frame.pc];
            ++frame.pc;
            return null;
        }
    }

    private static class DoRegIndex2 extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            state.indexReg = getIndex(frame.idata.itsICode, frame.pc);
            frame.pc += 2;
            return null;
        }
    }

    private static class DoRegIndex4 extends InstructionClass {
        @Override
        NewState execute(Context cx, CallFrame frame, InterpreterState state, int op) {
            state.indexReg = getInt(frame.idata.itsICode, frame.pc);
            frame.pc += 4;
            return null;
        }
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

    /**
     * Adds the new args to the bound args. Because we're unwrapping things in reverse order they
     * are actually prepended.
     */
    private static Object[] addBoundArgs(Object[] boundArgs, Object[] newArgs) {
        if (newArgs.length == 0) {
            return boundArgs;
        } else if (boundArgs == null) {
            return newArgs;
        } else {
            Object[] result = Arrays.copyOf(newArgs, boundArgs.length + newArgs.length);
            System.arraycopy(boundArgs, 0, result, newArgs.length, boundArgs.length);
            return result;
        }
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

    private static Scriptable getApplyThis(
            Context cx,
            Object[] stack,
            double[] sDbl,
            Object[] boundArgs,
            int thisIdx,
            int indexReg,
            Callable target,
            CallFrame frame) {
        Object obj;
        if (indexReg != 0) {
            if (boundArgs != null && boundArgs.length > 0) {
                obj = boundArgs[0];
            } else {
                obj = stack[thisIdx];
                if (obj == DOUBLE_MARK) obj = ScriptRuntime.wrapNumber(sDbl[thisIdx]);
            }
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
            ScriptOrFn fnOrScript,
            InterpreterData code,
            CallFrame parentFrame) {
        CallFrame frame =
                new CallFrame(
                        cx,
                        thisObj,
                        fnOrScript,
                        code,
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
        boolean usesActivation = frame.fnOrScript.getDescriptor().requiresActivationFrame();
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
        if (frame.fnOrScript.getDescriptor().requiresActivationFrame()) {
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

    private static JSFunction createClosure(Context cx, CallFrame frame, int index) {
        var desc = frame.fnOrScript.getDescriptor().getFunction(index);
        boolean isArrow = desc.getFunctionType() == FunctionNode.ARROW_FUNCTION;
        var homeObject = isArrow ? frame.fnOrScript.getHomeObject() : null;
        JSFunction f = new JSFunction(cx, frame.scope, desc, frame.thisObj, homeObject);
        return f;
    }

    private static JSFunction createMethod(
            Context cx, CallFrame frame, int index, Scriptable homeObject) {
        var desc = frame.fnOrScript.getDescriptor().getFunction(index);
        boolean isArrow = desc.getFunctionType() == FunctionNode.ARROW_FUNCTION;
        JSFunction f = new JSFunction(cx, frame.scope, desc, frame.thisObj, homeObject);
        return f;
    }
}

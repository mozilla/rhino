package org.mozilla.javascript;

import java.util.Arrays;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.debug.DebuggableScript;

/** Class to hold data corresponding to one interpreted call stack frame. */
final class CallFrame extends ACallFrame<CallFrame, InterpreterData<?>>
        implements Cloneable {
    private static final long serialVersionUID = -2843792508994958978L;

    boolean isContinuationsTopFrame;

    // The values that change during interpretation

    int pcPrevBranch;
    int pcSourceLineStart;

    int stackTop;
    int savedCallOp;

    CallFrame(
            Context cx,
            Scriptable thisObj,
            ScriptOrFn<?> fnOrScript,
            InterpreterData<?> code,
            CallFrame parentFrame,
            ACallFrame<?, ?> previousInterpreterFrame) {
        super(cx, thisObj, fnOrScript, code, parentFrame, previousInterpreterFrame);
        pcSourceLineStart = compilerData.firstLinePC;

        stackTop = emptyStackTop;
    }

    private CallFrame(CallFrame original, boolean makeOrphan) {
        this(
                original,
                makeOrphan ? null : original.parentFrame,
                makeOrphan ? null : original.previousInterpreterFrame);
    }

    /* Copy the frame for *continuations*. Here we want to make
    fresh copies of the stack and everything related to it. */
    CallFrame(
            CallFrame original, CallFrame parentFrame, ACallFrame<?, ?> previousInterpreterFrame) {
        super(original, parentFrame, previousInterpreterFrame);
        isContinuationsTopFrame = original.isContinuationsTopFrame;

        pcSourceLineStart = original.pcSourceLineStart;
        pcPrevBranch = original.pcPrevBranch;

        stackTop = original.stackTop;
        savedCallOp = original.savedCallOp;
        throwable = original.throwable;
    }

    /* Copy the stack for running a generator. We're only doing
    this to maintain the correct chain of parents for exception
    stacks, so we'll reuse the existing stack arrays. */
    CallFrame(
            CallFrame original,
            CallFrame parentFrame,
            ACallFrame<?, ?> previousInterpreterFrame,
            boolean keepFrozen) {
        super(original, parentFrame, previousInterpreterFrame, false, keepFrozen);
        isContinuationsTopFrame = original.isContinuationsTopFrame;

        pcSourceLineStart = original.pcSourceLineStart;
        pcPrevBranch = original.pcPrevBranch;

        stackTop = original.stackTop;
        savedCallOp = original.savedCallOp;
        throwable = original.throwable;
    }

    void initializeArgs(
            Context cx,
            VarScope callerScope,
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
                args = Interpreter.getArgsArray(args, argsDbl, boundArgs, blen, argShift, argCount);
            }
            argShift = 0;
            argsDbl = null;
            boundArgs = null;
        }

        if (desc.getFunctionType() != 0) {
            scope = fnOrScript.getDeclarationScope();

            if (useActivation) {
                if (desc.getFunctionType() == FunctionNode.ARROW_FUNCTION) {
                    scope =
                            ScriptRuntime.createArrowFunctionActivation(
                                    (JSFunction) fnOrScript,
                                    cx,
                                    scope,
                                    args,
                                    desc.hasRestArg(),
                                    desc.requiresArgumentObject());
                } else {
                    scope =
                            ScriptRuntime.createFunctionActivation(
                                    (JSFunction) fnOrScript,
                                    cx,
                                    scope,
                                    args,
                                    desc.hasRestArg(),
                                    desc.requiresArgumentObject());
                }
            }
        } else {
            scope = callerScope;
            ScriptRuntime.initScript(fnOrScript, thisObj, cx, scope, desc.isEvalFunction());
        }

        // Defer default parameters and nested function declarations until activation scope
        // creation
        // Ref: Ecma 2026, 10.2.11, FunctionDeclarationInstantiation

        if (desc.getFunctionCount() != 0 && !desc.isES6Generator()) {
            if (desc.getFunctionType() != 0 && !desc.requiresActivationFrame()) Kit.codeBug();
            for (int i = 0; i < desc.getFunctionCount(); i++) {
                JSDescriptor<?> fdesc = desc.getFunction(i);
                if (fdesc.getFunctionType() == FunctionNode.FUNCTION_STATEMENT) {
                    Interpreter.initFunction(cx, scope, fnOrScript.getDescriptor(), i);
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
            System.arraycopy(argsDbl, argShift, doubleStack, blen, definedArgs - blen);
        }
        for (int i = definedArgs; i != compilerData.maxVars; ++i) {
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

    @Override
    CallFrame cloneFrozen() {
        return new CallFrame(this, false);
    }

    CallFrame shallowCloneFrozen(ACallFrame<?,?> newPreviousInterpreeterFrame) {
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

        otherFrame.stackTop = stackTop;
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
                TopLevel top = ScriptableObject.getTopLevelScope(scope);
                Scriptable global = top.getGlobalThis();
                return ((Boolean)
                                ScriptRuntime.doTopCall(
                                        (c, scope, thisObj) -> equalsInTopScope(other),
                                        cx,
                                        top,
                                        global,
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
            h = 31 * (31 * h + f.pc) + f.compilerData.icodeHashCode();
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
                && Interpreter.compareDescs(
                        fnOrScript.getDescriptor(), other.fnOrScript.getDescriptor())
                && equal.equalGraphs(varSource.stack, other.varSource.stack)
                && Arrays.equals(varSource.doubleStack, other.varSource.doubleStack)
                && equal.equalGraphs(thisObj, other.thisObj)
                && equal.equalGraphs(fnOrScript, other.fnOrScript)
                && equal.equalGraphs(scope, other.scope);
    }

    CallFrame captureForGenerator() {
        return new CallFrame(this, true);
    }

    // ACallFrame implementation

    @Override
    public int getFrameIndex() {
        return frameIndex;
    }

    @Override
    public CallFrame getParentFrame() {
        return parentFrame;
    }

    @Override
    public int getPcSourceLineStart() {
        return pcSourceLineStart;
    }

    @Override
    public DebuggableScript getData() {
        return fnOrScript.getDescriptor();
    }

    @Override
    public int getParentPC() {
        return parentPC;
    }

    @Override
    public ACallFrame<?, ?> getPreviousInterpreterFrame() {
        return previousInterpreterFrame;
    }

    @Override
    public ScriptOrFn<?> getFnOrScript() {
        return fnOrScript;
    }
}

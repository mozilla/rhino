package org.mozilla.javascript;

import static org.mozilla.javascript.Interpreter.initFunction;
import static org.mozilla.javascript.UniqueTag.DOUBLE_MARK;

import java.io.Serializable;

import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.debug.DebuggableScript;
import org.mozilla.javascript.interpreterv2.CompilerData;
import org.mozilla.javascript.interpreterv2.GeneratorState;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class CallFrameV2 extends ACallFrame<CallFrameV2, CompilerData<?>> implements Serializable {

    public int pcPrevBranch;
    public boolean shouldYieldToParent;
    public GeneratorState generatorState;

    public CallFrameV2(
            Context cx,
            VarScope callerScope,
            Scriptable thisObj,
            Scriptable homeObj,
            Object[] args,
            double[] doubleArgs,
            int argShift,
            int argCount,
            ScriptOrFn<?> fnOrScript,
            CallFrameV2 parentFrame,
            ACallFrame<?, ?> previousInterpreterFrame) {
        super(cx, thisObj, fnOrScript, (CompilerData<?>) fnOrScript.getDescriptor().getCode(), parentFrame,
                previousInterpreterFrame);
        stackTop = emptyStackTop;

        // Start of initializeArgs()
        if (useActivation) {
            // Copy args to new array to pass to enterActivationFunction or debuggerFrame.onEnter
            if (doubleArgs != null) {
                args = wrapArguments(args, doubleArgs, argShift, argCount);
            }
            argShift = 0;
            doubleArgs = null;
        }

        JSDescriptor<?> desc = fnOrScript.getDescriptor();
        if (compilerData.functionType != CompilerData.FunctionType.Script) {
            scope = fnOrScript.getDeclarationScope();

            if (useActivation) {
                if (compilerData.functionType == CompilerData.FunctionType.ArrowFunction) {
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

        if (desc.nestedFunctions != null) {
            // Generators emit explicit ClosureStatement instructions for nested function
            // declarations after evaluating their default parameter expressions, so that
            // 'arguments' in a default-param scope binds to the arguments object rather
            // than to a hoisted inner `function arguments(){}`. Skip frame-init hoisting
            // for them.
            if (desc.getFunctionCount() != 0 && !desc.isES6Generator()) {
                if (desc.getFunctionType() != 0 && !desc.requiresActivationFrame()) Kit.codeBug();
                for (int i = 0; i < desc.getFunctionCount(); i++) {
                    JSDescriptor<?> fdesc = desc.getFunction(i);
                    if (fdesc.getFunctionType() == FunctionNode.FUNCTION_STATEMENT) {
                        initFunction(cx, this.scope, desc, i);
                    }
                }
            }
        }

        int varCount = desc.getParamAndVarCount();
        for (int i = 0; i < varCount; i++) {
            if (desc.getParamOrVarConst(i)) {
                this.stackAttributes[i] = (byte) ScriptableObject.CONST;
            }
        }
        int definedArgs = compilerData.argCount;
        if (definedArgs > argCount) {
            definedArgs = argCount;
        }

        // Fill the frame structure

        if (frameIndex > cx.getMaximumInterpreterStackDepth()) {
            throw Context.reportRuntimeError("Exceeded maximum stack depth");
        }

        frozen = false;

        System.arraycopy(args, argShift, stack, 0, definedArgs);
        if (doubleArgs != null) {
            System.arraycopy(doubleArgs, argShift, doubleStack, 0, definedArgs);
        }
        for (int i = definedArgs; i != compilerData.maxVars; ++i) {
            stack[i] = Undefined.instance;
        }

        if (compilerData.hasRestParams) {
            Object[] vals;
            int offset = compilerData.argCount - 1;
            if (argCount >= compilerData.argCount) {
                vals = new Object[argCount - offset];

                argShift = argShift + offset;
                for (int valsIdx = 0; valsIdx != vals.length; ++argShift, ++valsIdx) {
                    Object val = args[argShift];
                    if (val == UniqueTag.DOUBLE_MARK) {
                        val = ScriptRuntime.wrapNumber(doubleArgs[argShift]);
                    }
                    vals[valsIdx] = val;
                }
            } else {
                vals = ScriptRuntime.emptyArgs;
            }
            stack[offset] = cx.newArray(scope, vals);
        }
    }

    // Orphan copy (for generators) or regular copy
    private CallFrameV2(CallFrameV2 original, boolean makeOrphan) {
        this(
                original,
                makeOrphan ? null : original.parentFrame,
                makeOrphan ? null : original.previousInterpreterFrame);
    }

    // Full copy with new linkage
    private CallFrameV2(
            CallFrameV2 original, CallFrameV2 parentFrame, ACallFrame<?,?> previousInterpreterFrame) {
        super(original, parentFrame, previousInterpreterFrame);
        pcPrevBranch = original.pcPrevBranch;
        shouldYieldToParent = original.shouldYieldToParent;
        generatorState = original.generatorState;
    }

    /* Shallow copy for running a generator. Reuses the existing stack arrays. */
    private CallFrameV2(
            CallFrameV2 original,
            CallFrameV2 parentFrame,
            ACallFrame<?,?> previousInterpreterFrame,
            boolean keepFrozen) {
        super(original, parentFrame, previousInterpreterFrame, false, keepFrozen);
        pcPrevBranch = original.pcPrevBranch;
        shouldYieldToParent = original.shouldYieldToParent;
        generatorState = original.generatorState;
    }

    public void push(Object val) {
        stackTop += 1;
        stack[stackTop] = val;
    }

    public void push(int val) {
        stackTop += 1;
        stack[stackTop] = val;
    }

    public void push(double val) {
        stackTop += 1;
        stack[stackTop] = DOUBLE_MARK;
        doubleStack[stackTop] = val;
    }

    public void push(Object val, double doubleVal) {
        stackTop += 1;
        stack[stackTop] = val;
        doubleStack[stackTop] = doubleVal;
    }

    public void popResult() {
        result = stack[stackTop];
        resultDbl = doubleStack[stackTop];
        stack[stackTop] = null;
        stackTop -= 1;
    }

    public Object pop() {
        var value = stack[stackTop];
        stack[stackTop] = null;
        stackTop -= 1;
        return value;
    }

    public double popDouble() {
        assert stack[stackTop] == DOUBLE_MARK;
        var value = doubleStack[stackTop];
        stack[stackTop] = null;
        stackTop -= 1;
        return value;
    }

    public Object peek(int offset) {
        return stack[stackTop + offset];
    }

    public Object peek() {
        return peek(0);
    }

    public double peekDouble(int offset) {
        return doubleStack[stackTop + offset];
    }

    public double peekDouble() {
        return peekDouble(0);
    }

    public boolean isStackEmpty() {
        return stackTop == emptyStackTop;
    }

    public void saveExceptionScope(int exceptionIndex, VarScope scope) {
        stack[localShift + exceptionIndex] = scope;
    }

    public void saveSubRoutineReturnPC(int returnPcOffset, double subRoutineReturnPC) {
        stack[localShift + returnPcOffset] = DOUBLE_MARK;
        doubleStack[localShift + returnPcOffset] = subRoutineReturnPC;
    }

    public boolean hasSubRoutineReturnPC(int returnPcOffset) {
        return stack[localShift + returnPcOffset] == DOUBLE_MARK;
    }

    public double getSubRoutineReturnPC(int returnPcOffset) {
        if (stack[localShift + returnPcOffset] != DOUBLE_MARK) {
            throw new IllegalStateException("Use hasSubRoutineReturnPC first");
        }
        return doubleStack[localShift + returnPcOffset];
    }

    public Object getVarAndWrap(int index) {
        var value = getVar(index);
        if (value == DOUBLE_MARK) {
            return ScriptRuntime.wrapNumber(getVarDouble(index));
        }
        return value;
    }

    public Object getVar(int index) {
        return varSource.stack[index];
    }

    public boolean isVarDouble(int index) {
        return getVar(index) == DOUBLE_MARK;
    }

    public double getVarDouble(int index) {
        return varSource.doubleStack[index];
    }

    public void setVar(int index, Object value) {
        varSource.stack[index] = value;
    }

    public void setVar(int index, double value) {
        varSource.stack[index] = DOUBLE_MARK;
        varSource.doubleStack[index] = value;
    }

    public void setVar(int index, Object value, double doubleValue) {
        varSource.stack[index] = value;
        varSource.doubleStack[index] = doubleValue;
    }

    public int getVarAttribute(int index) {
        return varSource.stackAttributes[index];
    }

    public void setVarAttribute(int index, int attributes) {
        varSource.stackAttributes[index] &= (byte) ~attributes;
    }

    public Object getLocal(int index) {
        return stack[localShift + index];
    }

    public double getLocalDouble(int index) {
        return doubleStack[localShift + index];
    }

    public void setLocal(int index, Object value) {
        stack[localShift + index] = value;
    }

    public void setResult(Object value) {
        result = value;
    }

    public void setResult(double value) {
        result = DOUBLE_MARK;
        resultDbl = value;
    }

    // TODO(Cam): For object literals, we want to wrap literal values, but not getters and setters
    public Object[] getArguments(Context cx, Operand[] arguments) {
        if (arguments.length == 0) {
            return ScriptRuntime.emptyArgs;
        }
        Object[] args = new Object[arguments.length];
        for (int i = arguments.length - 1; i >= 0; i--) {
            args[i] = arguments[i].retrieveAndWrap(cx, this);
        }
        return args;
    }

    private static Object[] wrapArguments(
            Object[] stack, double[] sDbl, int shift, int count) {
        if (count == 0) {
            return ScriptRuntime.emptyArgs;
        }
        Object[] args = new Object[count];
        for (int i = 0; i != count; ++i, ++shift) {
            Object val = stack[shift];
            if (val == UniqueTag.DOUBLE_MARK) {
                val = ScriptRuntime.wrapNumber(sDbl[shift]);
            }
            args[i] = val;
        }
        return args;
    }

    @Override
    public int getFrameIndex() {
        return frameIndex;
    }

    @Override
    public CallFrameV2 getParentFrame() {
        return parentFrame;
    }

    @Override
    public int getPcSourceLineStart() {
        // This is not equivalent to frame v1, but it is still correct. This is used to find the
        // line number from the pc, which for frame v1 requires having the pc corresponding to the
        // LINE instruction, basically. However, with the line number table implementation that we
        // use in v2, it's ok if we use _any_ pc that corresponds to that same line number. So we
        // can simply return the current pc and get the correct behavior.
        return pc;
    }

    @Override
    public DebuggableScript getData() {
        return fnOrScript.getDescriptor();
    }

    @Override
    public ScriptOrFn<?> getFnOrScript() {
        return fnOrScript;
    }

    @Override
    public int getParentPC() {
        return parentPC;
    }

    @Override
    public ACallFrame getPreviousInterpreterFrame() {
        return previousInterpreterFrame;
    }

    public CallFrameV2 cloneFrozen() {
        return new CallFrameV2(this, false);
    }

    public CallFrameV2 captureForGenerator() {
        return new CallFrameV2(this, true);
    }

    /* Shallow clone for running a generator. We're only doing
    this to maintain the correct chain of parents for exception
    stacks, so we'll reuse the existing stack arrays. */
    public CallFrameV2 shallowCloneFrozen(ACallFrame newPreviousInterpreterFrame) {
        return new CallFrameV2(this, this.parentFrame, newPreviousInterpreterFrame, true);
    }

    public void syncStateToFrame(CallFrameV2 otherFrame) {
        otherFrame.frozen = frozen;
        otherFrame.result = result;
        otherFrame.resultDbl = resultDbl;
        otherFrame.pc = pc;
        otherFrame.pcPrevBranch = pcPrevBranch;
        otherFrame.scope = scope;

        otherFrame.stackTop = stackTop;
        otherFrame.throwable = throwable;
        otherFrame.shouldYieldToParent = shouldYieldToParent;
        otherFrame.generatorState = generatorState;
    }

    public void popN(int n) {
        for (int i = 0; i < n; i++) {
            pop();
        }
    }
}

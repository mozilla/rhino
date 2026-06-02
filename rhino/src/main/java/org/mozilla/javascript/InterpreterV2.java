package org.mozilla.javascript;

import static org.mozilla.javascript.UniqueTag.DOUBLE_MARK;

import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import org.mozilla.javascript.ast.ScriptNode;
import org.mozilla.javascript.debug.DebuggableScript;
import org.mozilla.javascript.interpreterv2.CompilerData;
import org.mozilla.javascript.interpreterv2.GeneratorState;
import org.mozilla.javascript.interpreterv2.instruction.JumpInstruction;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class InterpreterV2 extends AInterpreter<CallFrameV2, CompilerData<?>> {
    private static final int EX_CATCH_STATE = 2; // Can execute JS catch
    private static final int EX_FINALLY_STATE = 1; // Can execute JS finally
    private static final int EX_NO_JS_STATE = 0; // Terminate JS execution

    public static final int INVOCATION_COST = 100;
    // arbitrary exception cost for instruction counting
    private static final int EXCEPTION_COST = 100;

    static {
        RhinoException.registerInterpreterMethod(InterpreterV2.class, "interpretInner");
    }

    public InterpreterV2() {}

    public static Object interpret(Context cx, CallFrameV2 frame, Object throwable) {
        var oldFrame = cx.lastInterpreterFrame;
        try {
            return interpretInner(cx, frame, throwable);
        } finally {
            cx.lastInterpreterFrame = oldFrame;
        }
    }

    private static Object interpretInner(Context cx, CallFrameV2 frame, Object throwable) {
        cx.lastInterpreterFrame = frame;

        GeneratorState generatorState = null;
        if (throwable != null) {
            if (throwable instanceof GeneratorState) {
                generatorState = (GeneratorState) throwable;

                // reestablish this call frame
                enterFrame(cx, frame, ScriptRuntime.emptyArgs, true);
                frame.generatorState = generatorState;
                throwable = null;
            } else if (!(throwable instanceof ContinuationJump)) {
                // It should be continuation
                Kit.codeBug();
            }
        }

        boolean instructionCounting = cx.instructionThreshold != 0;
        // Try is here just because for dev this is nicer
        var instructions = frame.compilerData.instructions;
        int previousLineNumber = -1;
        while (frame.pc < instructions.length) {
            if (frame.throwable != null) {
                int exState = getExState(cx, frame.generatorState, frame.throwable);
                @SuppressWarnings("unchecked")
                var cjump =
                        frame.throwable instanceof ContinuationJump
                            ? (ContinuationJump<CallFrameV2, CompilerData<?>>) frame.throwable
                                : null;
                exState = handleDebugAndInstructionCount(cx, frame, exState);
                if (exState == EX_NO_JS_STATE) {
                    cjump = null;
                }
                var exceptionHandlerOffset = searchAndProcessFrame(cx, frame, exState, cjump);
                frame =
                        processThrowable(
                                cx,
                                frame.throwable,
                                frame,
                                exceptionHandlerOffset,
                                instructionCounting,
                                cjump);
                frame.throwable = null;
            }

            // We could make this faster via, for example, a subclass "DebuggableInterpreter" and a
            // virtual function... but I'm not sure if it would be worth it. Anyway, The JVM should
            // make this very cheap.
            if (cx.debugger != null) {
                // We have reached a new instruction - let's see if we have reached new lines as
                // well, and in that case invoke the debugger to trigger breakpoints
                List<Integer> lines = frame.compilerData.getLineSetFromPc(frame.pc);
                if (lines != null && !lines.isEmpty()) {
                    for (int line : lines) {
                        if (line != -1 && line != previousLineNumber) {
                            frame.debuggerFrame.onLineChange(cx, line);
                            previousLineNumber = line;
                        }
                    }
                }
            }

            try {
                var pcBefore = frame.pc;
                var instruction = instructions[frame.pc];
                instruction.interpret(cx, frame);

                assert instruction instanceof JumpInstruction
                                || frame.pc != pcBefore
                                || frame.throwable != null
                        : ("Instruction did not advance PC: "
                                + instruction.getClass().getName()
                                + " at PC: "
                                + frame.pc);

                if (frame.shouldYieldToParent) {
                    frame.shouldYieldToParent = false;
                    break;
                }

            } catch (Throwable ex) {
                if (frame.throwable != null) {
                    // This is serious bug and it is better to track it ASAP
                    ex.printStackTrace(System.err);
                    throw new IllegalStateException();
                }
                frame.throwable = ex;
            }
        }

        exitFrame(cx, frame, frame.throwable);
        var interpreterResult = frame.result;
        var interpreterResultDbl = frame.resultDbl;
        if (frame.parentFrame != null) {
            frame = frame.parentFrame;
            if (frame.frozen) {
                frame = frame.cloneFrozen();
            }
            // TODO(jimmy)
            // Need to figure out when this should happen
            // or if the way we do things makes this unnecessary
            // setCallResult(frame, interpreterResult, interpreterResultDbl);
        }

        return frame.result == DOUBLE_MARK
                ? ScriptRuntime.wrapNumber(frame.resultDbl)
                : frame.result;
    }

    private static int handleDebugAndInstructionCount(Context cx, CallFrameV2 frame, int exState) {
        boolean instructionCounting = cx.instructionThreshold != 0;
        var throwable = frame.throwable;
        if (instructionCounting) {
            try {
                addInstructionCount(cx, frame, EXCEPTION_COST);
            } catch (RuntimeException ex) {
                throwable = ex;
                exState = EX_FINALLY_STATE;
            } catch (Error ex) {
                // Error from instruction counting
                //     => unconditionally terminate JS
                frame.throwable = ex;
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
                frame.throwable = ex;
                exState = EX_NO_JS_STATE;
            }
        }
        return exState;
    }

    private static int searchAndProcessFrame(
            Context cx, CallFrameV2 frame, int exState, ContinuationJump cjump) {
        var throwable = frame.throwable;
        for (; ; ) {
            if (exState != EX_NO_JS_STATE) {
                boolean onlyFinally = (exState != EX_CATCH_STATE);
                var exceptionHandlerOffset = getExceptionHandler(frame, onlyFinally);
                if (exceptionHandlerOffset >= 0) {
                    // We caught an exception, restart the loop
                    // with exception pending the processing at the loop
                    // start
                    return exceptionHandlerOffset;
                }
            }
            // No allowed exception handlers in this frame, unwind
            // to parent and try to look there

            frame.throwable = null;
            exitFrame(cx, frame, throwable);

            frame = frame.parentFrame;
            if (frame == null) {
                break;
            }
            if (cjump != null && Objects.equals(cjump.branchFrame, frame)) {
                // Continuation branch point was hit,
                // restart the state loop to reenter continuation
                return -1;
            }
        }

        if (throwable instanceof RuntimeException) {
            throw (RuntimeException) throwable;
        }
        // Must be instance of Error or code bug
        throw (Error) throwable;
    }

    private static int getExState(Context cx, GeneratorState generatorState, Object throwable) {
        int exState;

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
        } else {
            exState =
                    cx.hasFeature(Context.FEATURE_ENHANCED_JAVA_ACCESS)
                            ? EX_CATCH_STATE
                            : EX_FINALLY_STATE;
        }
        return exState;
    }

    private static CallFrameV2 processThrowable(
            Context cx,
            Object throwable,
            CallFrameV2 frame,
            int exceptionHandlerOffset,
            boolean instructionCounting,
            ContinuationJump<CallFrameV2, CompilerData<?>> cjump) {
        // Recovering from exception, exceptionHandlerOffset contains
        // the index of handler

        if (exceptionHandlerOffset >= 0) {
            // Normal exception handler, transfer
            // control appropriately

            if (frame.frozen) {
                // XXX Deal with exceptios!!!
                frame = frame.cloneFrozen();
            }

            int[] table = frame.compilerData.exceptionTable;

            frame.pc = table[exceptionHandlerOffset + CompilerData.EXCEPTION_HANDLER_SLOT];
            if (instructionCounting) {
                frame.pcPrevBranch = frame.pc;
            }

            frame.stackTop = frame.emptyStackTop;
            int scopeLocal =
                    frame.localShift
                            + table[exceptionHandlerOffset + CompilerData.EXCEPTION_SCOPE_SLOT];
            int exLocal =
                    frame.localShift
                            + table[exceptionHandlerOffset + CompilerData.EXCEPTION_LOCAL_SLOT];
            frame.scope = (VarScope) frame.stack[scopeLocal];
            frame.stack[exLocal] = throwable;

        } else {
            // Continuation restoration
            // Interpreter.ContinuationJump cjump = (Interpreter.ContinuationJump) throwable;
            //
            // Clear throwable to indicate that exceptions are OK

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
            CallFrameV2[] enterFrames = null;

            CallFrameV2 x = cjump.capturedFrame;
            for (int i = 0; i != rewindCount; ++i) {
                if (!x.frozen) Kit.codeBug();
                if (isFrameEnterExitRequired(x)) {
                    if (enterFrames == null) {
                        // Allocate enough space to store the rest
                        // of rewind frames in case all of them
                        // would require to enter
                        enterFrames = new CallFrameV2[rewindCount - i];
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
        return frame;
    }

    private static boolean isFrameEnterExitRequired(CallFrameV2 frame) {
        return frame.debuggerFrame != null || frame.compilerData.needsActivation;
    }

    private static void setCallResult(CallFrameV2 frame, Object callResult, double callResultDbl) {
        throw new UnsupportedOperationException("Haven't implemented all the continuations stuff");
        // TODO: Need to think about how to represent this given our setup
        // if (frame.savedCallOp == Token.CALL || frame.savedCallOp == Icode_CALL_ON_SUPER) {
        //     frame.stack[frame.savedStackTop] = callResult;
        //     frame.doubleStack[frame.savedStackTop] = callResultDbl;
        // } else if (frame.savedCallOp == Token.NEW) {
        //     // If construct returns scriptable,
        //     // then it replaces on stack top saved original instance
        //     // of the object.
        //     if (ScriptRuntime.isActualScriptable(callResult)) {
        //         frame.stack[frame.savedStackTop] = callResult;
        //     }
        // } else {
        //     Kit.codeBug();
        // }
        // frame.savedCallOp = 0;
    }

    private static int getExceptionHandler(CallFrameV2 frame, boolean onlyFinally) {
        int[] exceptionTable = frame.compilerData.exceptionTable;
        if (exceptionTable == null) {
            // No exception handlers
            return -1;
        }

        // OPT: use binary search
        int best = -1, bestStart = 0, bestEnd = 0;
        for (int i = 0; i != exceptionTable.length; i += CompilerData.EXCEPTION_SLOT_SIZE) {
            int start = exceptionTable[i + CompilerData.EXCEPTION_TRY_START_SLOT];
            int end = exceptionTable[i + CompilerData.EXCEPTION_TRY_END_SLOT];
            if (!(start <= frame.pc && frame.pc < end)) {
                continue;
            }
            if (onlyFinally && exceptionTable[i + CompilerData.EXCEPTION_TYPE_SLOT] != 1) {
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

    public static boolean shouldOverrideNativeCall(
            Object securityDomain, Callable fun, Scriptable funThisObj) {
        if (fun instanceof KnownBuiltInFunction
                && BaseFunction.isApplyOrCall((KnownBuiltInFunction) fun)) {
            Callable applyCallable = ScriptRuntime.getCallable(funThisObj);
            if (applyCallable instanceof JSFunction) {
                JSFunction iApplyCallable = (JSFunction) applyCallable;
                if (iApplyCallable.getDescriptor().getCode() instanceof CompilerData
                        && securityDomain == iApplyCallable.getDescriptor().getSecurityDomain())
                    return false;
            }
        }

        return true;
    }

    public static void initFunction(Context cx, VarScope scope, JSDescriptor<?> parent, int index) {
        JSFunction fn = JSFunction.createFunction(cx, scope, parent, index, null);
        ScriptRuntime.initFunction(
                cx, scope, fn, fn.getDescriptor().getFunctionType(), parent.isEvalFunction());
    }

    public static <T extends ScriptOrFn<T>> Object interpret(
            T fun,
            CompilerData<T> data,
            Context cx,
            VarScope scope,
            Scriptable thisObj,
            Object[] args) {
        if (!ScriptRuntime.hasTopCall(cx)) {
            Kit.codeBug();
        }

        JSDescriptor<T> desc = fun.getDescriptor();
        SecurityController securityController = desc.getSecurityController();
        Object securityDomain = desc.getSecurityDomain();
        if (securityController != null && cx.interpreterSecurityDomain != securityDomain) {
            Object savedDomain = cx.interpreterSecurityDomain;
            cx.interpreterSecurityDomain = securityDomain;
            try {
                return securityController.callWithDomain(
                        securityDomain, cx, (Callable) fun, scope, thisObj, args);
            } finally {
                cx.interpreterSecurityDomain = savedDomain;
            }
        }

        var frame =
                new CallFrameV2(
                        cx,
                        scope,
                        thisObj,
                        fun.getHomeObject(),
                        args,
                        null,
                        0,
                        args.length,
                        fun,
                        null,
                        cx.lastInterpreterFrame);

        boolean parentStrictness = ScriptRuntime.enterFunctionStrictness(cx, desc.isStrict());
        try {
            enterFrame(cx, frame, args, false);
            return InterpreterV2.interpret(cx, frame, null);
        } finally {
            ScriptRuntime.exitFunctionStrictness(cx, parentStrictness);
        }
    }

    public static Object resumeGenerator(
            Context cx, VarScope scope, int operation, Object savedState, Object value) {
        CallFrameV2 frame = (CallFrameV2) savedState;
        CallFrameV2 activeFrame = frame.shallowCloneFrozen(cx.lastInterpreterFrame);
        try {
            GeneratorState generatorState = new GeneratorState(operation, value);
            if (operation == NativeGenerator.GENERATOR_CLOSE) {
                try {
                    return interpret(cx, activeFrame, generatorState);
                } catch (NativeGenerator.GeneratorClosedException e) {
                    // Re-throw GeneratorClosedException so ES6Generator can catch and complete
                    throw e;
                } catch (RuntimeException e) {
                    // Only propagate exceptions other than closingException
                    if (e != value) throw e;
                }
                return Undefined.instance;
            }
            Object result = interpret(cx, activeFrame, generatorState);
            if (generatorState.returnedException != null) throw generatorState.returnedException;
            return result;
        } finally {
            activeFrame.syncStateToFrame(frame);
        }
    }

    public static boolean doShallowEquals(
            Context cx, CallFrameV2 frame, Operand left, Operand right) {
        Object rhs;
        double rDouble = 0.0;
        if (right.isDouble(frame)) {
            rDouble = right.retrieveDouble(frame);
            rhs = DOUBLE_MARK;
        } else {
            rhs = right.retrieve(cx, frame);
        }
        Object lhs;
        double lDouble = 0.0;
        if (left.isDouble(frame)) {
            lDouble = left.retrieveDouble(frame);
            lhs = DOUBLE_MARK;
        } else {
            lhs = left.retrieve(cx, frame);
        }
        if (rhs == DOUBLE_MARK) {
            if (lhs instanceof Number && !(lhs instanceof BigInteger)) {
                lDouble = ((Number) lhs).doubleValue();
            } else if (lhs != DOUBLE_MARK) {
                return false;
            }
        } else if (lhs == DOUBLE_MARK) {
            if (rhs instanceof Number && !(rhs instanceof BigInteger)) {
                rDouble = ((Number) rhs).doubleValue();
            } else {
                return false;
            }
        } else {
            return ScriptRuntime.shallowEq(lhs, rhs);
        }
        return (lDouble == rDouble);
    }

    public static boolean doEquals(Context cx, CallFrameV2 frame, Operand left, Operand right) {
        Object rhs;
        double rDouble = 0.0;
        if (right.isDouble(frame)) {
            rDouble = right.retrieveDouble(frame);
            rhs = DOUBLE_MARK;
        } else {
            rhs = right.retrieve(cx, frame);
        }
        Object lhs;
        double lDouble = 0.0;
        if (left.isDouble(frame)) {
            lDouble = left.retrieveDouble(frame);
            lhs = DOUBLE_MARK;
        } else {
            lhs = left.retrieve(cx, frame);
        }
        if (rhs == DOUBLE_MARK) {
            if (lhs == DOUBLE_MARK) {
                return (lDouble == rDouble);
            }
            return ScriptRuntime.eqNumber(rDouble, lhs);
        }
        if (lhs == DOUBLE_MARK) {
            return ScriptRuntime.eqNumber(lDouble, rhs);
        }
        return ScriptRuntime.eq(lhs, rhs);
    }

    @Override
    public CompilationResult<JSScript> compileScript(
            CompilerEnvirons compilerEnv, ScriptNode tree, String rawSource) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompilationResult<JSFunction> compileFunction(
            CompilerEnvirons compilerEnv, ScriptNode tree, String rawSource) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Script createScriptObject(
            CompilationResult<JSScript> compiled, Object staticSecurityDomain) {
        var result = (V2CompilationResult<JSScript>) compiled;
        return JSFunction.createScript(result.data, result.homeObject, staticSecurityDomain);
    }

    @Override
    public Function createFunctionObject(
            Context cx,
            VarScope scope,
            CompilationResult<JSFunction> compiled,
            Object staticSecurityDomain) {
        var result = (V2CompilationResult<JSFunction>) compiled;
        return JSFunction.createFunction(
                cx, scope, result.data, result.homeObject, staticSecurityDomain);
    }

    public static void addInstructionCount(Context cx, CallFrameV2 frame, int extra) {
        cx.instructionCount += frame.pc - frame.pcPrevBranch + extra;
        if (cx.instructionCount > cx.instructionThreshold) {
            cx.observeInstructionCount(cx.instructionCount);
            cx.instructionCount = 0;
        }
    }

    private static class V2CompilationResult<T extends ScriptOrFn<T>>
            implements CompilationResult<T> {
        private final JSDescriptor<T> data;
        private final Scriptable homeObject;

        V2CompilationResult(JSDescriptor<T> data, Scriptable homeObject) {
            this.data = data;
            this.homeObject = homeObject;
        }

        @Override
        public DebuggableScript getDebuggableScript() {
            return data;
        }
    }
}

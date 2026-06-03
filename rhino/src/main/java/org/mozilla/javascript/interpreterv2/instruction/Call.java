package org.mozilla.javascript.interpreterv2.instruction;

import static org.mozilla.javascript.InterpreterV2.INVOCATION_COST;
import static org.mozilla.javascript.InterpreterV2.addInstructionCount;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.VarScope;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

/**
 * The common case is {@link Type#Call} with a small, fixed number of arguments. For those we use
 * arity-specialized subclasses ({@link Call0}..{@link Call3}) that store their argument operands in
 * fields instead of a heap-allocated {@code Operand[]}, which saves both the array object and its
 * header for the bulk of all call sites.
 *
 * <p>Everything else (4+ arguments, or any of the rarer {@link Type#CallOnSuper}/{@link
 * Type#TailCall}/{@link Type#RefCall} variants) goes through the generic {@link CallN}, which keeps
 * the argument array and the {@link Type} field.
 */
public abstract class Call extends Instruction {
    public enum Type {
        Call,
        CallOnSuper,
        TailCall,
        RefCall,
    }

    final Operand lookupResult;

    Call(Operand lookupResult) {
        this.lookupResult = lookupResult;
    }

    public static Call create(Operand lookupResult, Operand[] arguments, Type callType) {
        if (callType == Type.Call) {
            switch (arguments.length) {
                case 0:
                    return new Call0(lookupResult);
                case 1:
                    return new Call1(lookupResult, arguments[0]);
                case 2:
                    return new Call2(lookupResult, arguments[0], arguments[1]);
                case 3:
                    return new Call3(lookupResult, arguments[0], arguments[1], arguments[2]);
                default:
                    break;
            }
        }
        return new CallN(lookupResult, arguments, callType);
    }

    protected void countInvocation(Context cx, CallFrameV2 frame) {
        if (cx.getInstructionObserverThreshold() != 0) {
            addInstructionCount(cx, frame, INVOCATION_COST);
        }
    }

    /**
     * Shared dispatch for a plain {@link Type#Call}. {@code args} must already be fully evaluated
     * (the arguments sit above {@link #lookupResult} on the stack, so they are retrieved before the
     * lookup result is popped).
     */
    protected final void doPlainCall(Context cx, CallFrameV2 frame, Object[] args) {
        // CALL generation ensures that fun and funThisObj
        // are already Scriptable and Callable objects respectively
        var result = (ScriptRuntime.LookupResult) lookupResult.retrieve(cx, frame);
        Scriptable funThisObj = result.getThis();
        Callable fun = result.getCallable();

        VarScope calleeScope = frame.scope;
        if (frame.useActivation) {
            calleeScope = ScriptableObject.getTopLevelScope(frame.scope);
        }

        cx.lastInterpreterFrame = frame;
        frame.push(fun.call(cx, calleeScope, funThisObj, args));
    }

    /** Shared {@link #toDebugString()} rendering for the plain-call subclasses. */
    String formatDebug(Operand[] args) {
        return InstructionFormatter.formatInstruction(
                "Call", "callType", Type.Call, "lookupResult", lookupResult, "args", args);
    }
}

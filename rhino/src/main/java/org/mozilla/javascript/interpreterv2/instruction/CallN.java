package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

/**
 * Generic call instruction: an arbitrary number of arguments stored in an array, and any {@link
 * Call.Type}. Used for 4+ argument calls and for the rarer non-plain call types. The common plain,
 * low-arity case is handled by the arity-specialized {@link Call0}..{@link Call3} instead.
 */
final class CallN extends Call {
    private final Operand[] arguments;
    private final Call.Type callType;

    CallN(Operand lookupResult, Operand[] arguments, Call.Type callType) {
        super(lookupResult);
        this.arguments = arguments;
        this.callType = callType;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;
        countInvocation(cx, frame);

        // TODO(Cam):
        //  Need to implement all the edge cases. Mostly with continuations

        // CALL generation ensures that fun and funThisObj
        // are already Scriptable and Callable objects respectively
        Object[] args = frame.getArguments(cx, arguments);
        var result = (ScriptRuntime.LookupResult) lookupResult.retrieve(cx, frame);
        Scriptable funThisObj = result.getThis();
        Callable fun = result.getCallable();
        if (callType == Call.Type.CallOnSuper) {
            // funThisObj would have been the "super" object, which we
            // used to lookup the function. Now that that's done, we
            // discard it and invoke the function with the current
            // "this".
            funThisObj = frame.thisObj;
        }

        if (callType == Call.Type.RefCall) {
            frame.push(ScriptRuntime.callRef(fun, funThisObj, args, cx));
            return;
        }

        cx.lastInterpreterFrame = frame;
        frame.push(fun.call(cx, frame.scope, funThisObj, args));
    }

    @Override
    public int stackChange() {
        int count = 0;
        for (var argument : arguments) {
            count += argument.stackChange();
        }
        return 1 + lookupResult.stackChange() + count;
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(
                "Call", "callType", callType, "lookupResult", lookupResult, "args", arguments);
    }
}

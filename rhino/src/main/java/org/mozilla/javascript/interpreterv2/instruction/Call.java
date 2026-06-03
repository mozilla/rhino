package org.mozilla.javascript.interpreterv2.instruction;

import static org.mozilla.javascript.InterpreterV2.INVOCATION_COST;
import static org.mozilla.javascript.InterpreterV2.addInstructionCount;

import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.VarScope;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class Call extends Instruction {
    public enum Type {
        Call,
        CallOnSuper,
        TailCall,
        RefCall,
    }

    private final Operand lookupResult;
    private final Operand[] arguments;
    private final Type callType;

    public Call(Operand lookupResult, Operand[] arguments, Type callType) {
        this.lookupResult = lookupResult;
        this.arguments = arguments;
        this.callType = callType;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;

        if (cx.getInstructionObserverThreshold() != 0) {
            addInstructionCount(cx, frame, INVOCATION_COST);
        }

        // TODO(Cam):
        //  Need to implement all the edge cases. Mostly with continuations

        // CALL generation ensures that fun and funThisObj
        // are already Scriptable and Callable objects respectively
        Object[] args = frame.getArguments(cx, arguments);
        var result = (ScriptRuntime.LookupResult) lookupResult.retrieve(cx, frame);
        Scriptable funThisObj = result.getThis();
        Callable fun = result.getCallable();
        Scriptable funHomeObj =
                (fun instanceof BaseFunction) ? ((BaseFunction) fun).getHomeObject() : null;
        if (callType == Type.CallOnSuper) {
            // funThisObj would have been the "super" object, which we
            // used to lookup the function. Now that that's done, we
            // discard it and invoke the function with the current
            // "this".
            funThisObj = frame.thisObj;
        }

        if (callType == Type.RefCall) {
            frame.push(ScriptRuntime.callRef(fun, funThisObj, args, cx));
            return;
        }
        VarScope calleeScope = frame.scope;
        if (frame.useActivation) {
            calleeScope = ScriptableObject.getTopLevelScope(frame.scope);
        }

        cx.lastInterpreterFrame = frame;
        frame.push(fun.call(cx, calleeScope, funThisObj, args));
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
                this, "callType", callType, "lookupResult", lookupResult, "args", arguments);
    }
}

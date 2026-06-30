package org.mozilla.javascript.interpreterv2.instruction;

import static org.mozilla.javascript.InterpreterV2.INVOCATION_COST;
import static org.mozilla.javascript.InterpreterV2.addInstructionCount;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class SpecialCall extends Instruction {
    private final Operand lookupResult;
    private final Operand[] arguments;
    private final short lineNumber;
    private final int callType;

    public SpecialCall(Operand lookupResult, Operand[] arguments, short lineNumber, int callType) {
        this.lookupResult = lookupResult;
        this.arguments = arguments;
        this.lineNumber = lineNumber;
        this.callType = callType;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;

        if (cx.getInstructionObserverThreshold() != 0) {
            addInstructionCount(cx, frame, INVOCATION_COST);
        }

        // Call code generation ensure that stack here is ... Callable Scriptable
        Object[] args = frame.getArguments(cx, arguments);
        var result = (ScriptRuntime.LookupResult) lookupResult.retrieve(cx, frame);
        Callable function = result.getCallable();

        frame.push(
                ScriptRuntime.callSpecial(
                        cx,
                        function,
                        result.getThis(),
                        args,
                        frame.scope,
                        frame.thisObj,
                        callType,
                        frame.fnOrScript.getDescriptor().getSourceName(),
                        lineNumber,
                        // TODO(Cam):
                        //  Doesn't look like Icode_CALLSPECIAL_OPTIONAL is ever placed anywhere, so
                        //  this last param will always be false
                        false));
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
                this,
                "lookupResult",
                lookupResult,
                "args",
                arguments,
                "line",
                lineNumber,
                "callType",
                callType);
    }
}

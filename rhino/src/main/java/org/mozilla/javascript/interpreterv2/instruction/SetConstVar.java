package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class SetConstVar extends Instruction {
    private final int index;
    private final Operand value;

    public SetConstVar(int index, Operand value) {
        this.index = index;
        this.value = value;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        if ((frame.getVarAttribute(index) & ScriptableObject.READONLY) == 0) {
            String msg =
                    ScriptRuntime.getMessageById(
                            "msg.var.redecl",
                            frame.fnOrScript.getDescriptor().getParamOrVarName(index));
            throw Context.reportRuntimeError(msg);
        }
        if ((frame.getVarAttribute(index) & ScriptableObject.UNINITIALIZED_CONST) != 0) {
            if (value.isDouble(frame)) {
                var doubleValue = value.retrieveDouble(frame);
                frame.setVar(index, doubleValue);
                frame.push(doubleValue);
            } else {
                var value = this.value.retrieve(cx, frame);
                frame.setVar(index, value);
                frame.push(value);
            }
            frame.setVarAttribute(index, ScriptableObject.UNINITIALIZED_CONST);
        }
        frame.pc += 1;
    }

    @Override
    public int stackChange() {
        return 1 + value.stackChange();
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "index", index, "value", value);
    }
}

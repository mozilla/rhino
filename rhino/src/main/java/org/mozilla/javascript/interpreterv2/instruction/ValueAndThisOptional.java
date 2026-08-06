package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class ValueAndThisOptional extends Instruction {
    private final Operand value;

    public ValueAndThisOptional(Operand value) {
        this.value = value;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;

        var value = this.value.retrieveAndWrap(cx, frame);
        frame.push(ScriptRuntime.getValueAndThisOptional(value, cx));
    }

    @Override
    public int stackChange() {
        return 1 + value.stackChange();
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "value", value);
    }
}

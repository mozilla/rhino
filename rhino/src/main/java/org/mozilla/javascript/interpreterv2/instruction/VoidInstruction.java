package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;
import org.mozilla.javascript.interpreterv2.operand.PopOperand;

public class VoidInstruction extends Instruction {
    private static final VoidInstruction voidInstructionPopOperand =
            new VoidInstruction(PopOperand.instance);

    public static VoidInstruction ofOperand(Operand obj) {
        if (obj == PopOperand.instance) {
            return voidInstructionPopOperand;
        } else {
            return new VoidInstruction(obj);
        }
    }

    private final Operand obj;

    private VoidInstruction(Operand obj) {
        this.obj = obj;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;
        obj.retrieve(cx, frame);
    }

    @Override
    public int stackChange() {
        return obj.stackChange();
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "obj", obj);
    }
}

package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;
import org.mozilla.javascript.interpreterv2.operand.PopOperand;

public class Not extends Instruction {
    private static final Not notPopOperand = new Not(PopOperand.instance);

    public static Not ofOperand(Operand obj) {
        if (obj == PopOperand.instance) {
            return notPopOperand;
        } else {
            return new Not(obj);
        }
    }

    private final Operand obj;

    private Not(Operand obj) {
        this.obj = obj;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;
        var obj = ScriptRuntime.wrapBoolean(this.obj.coerceToBoolean(cx, frame));
        frame.push(!obj);
    }

    @Override
    public int stackChange() {
        return 1 + obj.stackChange();
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "obj", obj);
    }
}

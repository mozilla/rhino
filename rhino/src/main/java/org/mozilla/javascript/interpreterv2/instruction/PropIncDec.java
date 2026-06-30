package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class PropIncDec extends Instruction {
    private final Operand obj;
    private final String property;
    private final int incrDecrMask;

    public PropIncDec(Operand obj, String property, int incrDecrMask) {
        this.obj = obj;
        this.property = property;
        this.incrDecrMask = incrDecrMask;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;

        var lhs = obj.retrieveAndWrap(cx, frame);
        frame.push(ScriptRuntime.propIncrDecr(lhs, property, cx, frame.scope, incrDecrMask));
    }

    @Override
    public int stackChange() {
        return 1 + obj.stackChange();
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(
                this, "obj", obj, "property", property, "mask", incrDecrMask);
    }
}

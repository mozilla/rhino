package org.mozilla.javascript.interpreterv2.instruction;

import java.math.BigInteger;
import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class Neg extends Instruction {
    private final Operand obj;

    public Neg(Operand obj) {
        this.obj = obj;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;

        var value = this.obj.retrieveNumber(cx, frame);

        var result = ScriptRuntime.negate(value);

        if (result instanceof BigInteger) {
            frame.push(result);
        } else {
            frame.push(result.doubleValue());
        }
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

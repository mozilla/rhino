package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.InstructionSimplification;
import org.mozilla.javascript.interpreterv2.KnownType;
import org.mozilla.javascript.interpreterv2.operand.Operand;

/** Pushes the operand's numeric value incremented or decremented by one. */
public class NumericIncDec extends Instruction {
    private final Operand obj;
    private final int incrDecrMask;

    public NumericIncDec(Operand obj, int incrDecrMask) {
        this.obj = obj;
        this.incrDecrMask = incrDecrMask;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        if (obj.isDouble(frame)) {
            double d = obj.retrieveDouble(frame);
            frame.push((incrDecrMask & Node.DECR_FLAG) == 0 ? d + 1.0 : d - 1.0);
        } else {
            Number result =
                    ScriptRuntime.incrDecrNumber(obj.retrieveNumber(cx, frame), incrDecrMask);
            if (result instanceof Double) {
                frame.push(result.doubleValue());
            } else {
                frame.push(result);
            }
        }
        frame.pc += 1;
    }

    @Override
    public int stackChange() {
        return 1 + obj.stackChange();
    }

    @Override
    public KnownType getKnownType(InstructionSimplification simplifier) {
        return KnownType.NUMBER;
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "obj", obj, "mask", incrDecrMask);
    }
}

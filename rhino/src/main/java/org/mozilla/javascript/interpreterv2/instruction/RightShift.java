package org.mozilla.javascript.interpreterv2.instruction;

import static org.mozilla.javascript.ScriptRuntime.toInt32;

import java.math.BigInteger;
import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class RightShift extends Instruction {
    private final Operand lhs;
    private final Operand rhs;

    public RightShift(Operand lhs, Operand rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;

        Object rObj = null;
        Number rValue = null;

        Object lObj = null;
        Number lValue = null;

        if (rhs.isDouble(frame)) {
            var r = rhs.retrieveDouble(frame);
            if (lhs.isDouble(frame)) {
                var l = lhs.retrieveDouble(frame);
                int result = toInt32(l) >> (toInt32(r) & 0x1f);
                frame.push((double) result);
                return;
            } else {
                rValue = r;
                lObj = lhs.retrieve(cx, frame);
            }
        } else {
            rObj = rhs.retrieve(cx, frame);
            if (lhs.isDouble(frame)) {
                lValue = lhs.retrieveDouble(frame);
            } else {
                lObj = lhs.retrieve(cx, frame);
            }
        }

        if (lValue == null) {
            lValue = ScriptRuntime.toNumeric(lObj);
        }

        if (rValue == null) {
            rValue = ScriptRuntime.toNumeric(rObj);
        }

        var result = ScriptRuntime.signedRightShift(lValue, rValue);

        if (result instanceof BigInteger) {
            frame.push(result);
        } else {
            frame.push(result.doubleValue());
        }
    }

    @Override
    public int stackChange() {
        return 1 + lhs.stackChange() + rhs.stackChange();
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "lhs", lhs, "rhs", rhs);
    }
}

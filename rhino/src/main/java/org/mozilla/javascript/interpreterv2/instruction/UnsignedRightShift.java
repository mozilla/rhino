package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class UnsignedRightShift extends Instruction {
    private final Operand lhs;
    private final Operand rhs;

    public UnsignedRightShift(Operand lhs, Operand rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;

        double rValue = 0;
        Object rObj = null;
        boolean shouldCoerceRight = false;
        if (rhs.isDouble(frame)) {
            rValue = rhs.retrieveDouble(frame);
        } else {
            rObj = rhs.retrieve(cx, frame);
            shouldCoerceRight = true;
        }
        double lValue = 0;
        Object lObj = null;
        boolean shouldCoerceLeft = false;
        if (lhs.isDouble(frame)) {
            lValue = lhs.retrieveDouble(frame);
        } else {
            lObj = lhs.retrieve(cx, frame);
            shouldCoerceLeft = true;
        }

        if (shouldCoerceLeft) {
            lValue = ScriptRuntime.toNumber(lObj);
        }
        if (shouldCoerceRight) {
            rValue = ScriptRuntime.toInt32(rObj);
        }

        long value = ScriptRuntime.toUint32(lValue) >>> ((int) rValue & 0x1F);
        // This cast is implicit in the original interpreter
        frame.push((double) value);
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

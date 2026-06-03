package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class StartSubroutine extends Instruction {
    private final int subReturnOffset;
    private final Operand returnPcOperand;

    public StartSubroutine(int subReturnOffset, Operand returnPcOperand) {
        this.subReturnOffset = subReturnOffset;
        this.returnPcOperand = returnPcOperand;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;

        if (!frame.isStackEmpty()) {
            // We are called from GoSubroutine, that adds the stack
            // This is behavior in the original interpreter
            // not a fan of this setup at all. But leaving it
            // until we have a better setup
            var returnPc = returnPcOperand.retrieveDouble(frame);
            frame.saveSubRoutineReturnPC(subReturnOffset, returnPc);
        }
    }

    @Override
    public int stackChange() {
        return 0;
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(
                this, "subReturnOffset", subReturnOffset, "returnPc", returnPcOperand);
    }
}

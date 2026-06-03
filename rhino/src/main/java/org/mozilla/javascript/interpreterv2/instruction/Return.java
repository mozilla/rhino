package org.mozilla.javascript.interpreterv2.instruction;

import static org.mozilla.javascript.InterpreterV2.addInstructionCount;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;

public class Return extends Instruction {
    public static final Return instance = new Return();

    private Return() {}

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        // Count remaining instructions before setting pc to MAX_VALUE
        if (cx.getInstructionObserverThreshold() != 0) {
            addInstructionCount(cx, frame, 0);
        }
        frame.pc = Integer.MAX_VALUE;
        frame.popResult();
    }

    @Override
    public int stackChange() {
        return -1;
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this);
    }
}

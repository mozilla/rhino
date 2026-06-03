package org.mozilla.javascript.interpreterv2.instruction;

import static org.mozilla.javascript.InterpreterV2.addInstructionCount;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;

public class ReturnUndefined extends Instruction {
    public static final ReturnUndefined instance = new ReturnUndefined();

    private ReturnUndefined() {}

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        // Count remaining instructions before setting pc to MAX_VALUE
        if (cx.getInstructionObserverThreshold() != 0) {
            addInstructionCount(cx, frame, 0);
        }
        frame.pc = Integer.MAX_VALUE;
        frame.setResult(Undefined.instance);
    }

    @Override
    public int stackChange() {
        return 0;
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this);
    }
}

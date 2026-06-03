package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;

public class ShortNumber extends Instruction {
    private final short num;

    public ShortNumber(short num) {
        this.num = num;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        // Why is this the logic? We took it from the old interpreter,
        // but the comparison to -1.0 seems odd.
        if (num != -1.0) {
            frame.push(num);
        } else {
            frame.push((double) num);
        }
        frame.pc += 1;
    }

    @Override
    public int stackChange() {
        return 1;
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "value", num);
    }
}

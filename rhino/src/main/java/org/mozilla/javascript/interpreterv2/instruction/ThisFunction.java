package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;

public class ThisFunction extends Instruction {
    public static final ThisFunction instance = new ThisFunction();

    private ThisFunction() {}

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.push(frame.fnOrScript);
        frame.pc += 1;
    }

    @Override
    public int stackChange() {
        return 1;
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this);
    }
}

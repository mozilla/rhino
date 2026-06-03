package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;

public class Pop extends Instruction {
    public static final Pop instance = new Pop();

    private Pop() {}

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pop();
        frame.pc += 1;
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

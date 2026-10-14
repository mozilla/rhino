package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;

public class Dup extends Instruction {
    public static final Dup instance = new Dup();

    private Dup() {}

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;
        frame.push(frame.peek(), frame.peekDouble());
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

package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;

public class DebuggerInstruction extends Instruction {
    public static final DebuggerInstruction instance = new DebuggerInstruction();

    private DebuggerInstruction() {}

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        if (frame.debuggerFrame != null) {
            frame.debuggerFrame.onDebuggerStatement(cx);
        }
        frame.pc += 1;
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

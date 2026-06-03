package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;

public class LeaveWith extends Instruction {
    public static final LeaveWith instance = new LeaveWith();

    private LeaveWith() {}

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.scope = ScriptRuntime.leaveWith(frame.scope);

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

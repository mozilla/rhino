package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;

public class LeaveScope extends Instruction {
    public static final LeaveScope instance = new LeaveScope();

    private LeaveScope() {}

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.scope = ScriptRuntime.leaveScope(frame.scope);

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

package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;

public class LocalLoad extends Instruction {
    private final int localIndex;

    public LocalLoad(int localIndex) {
        this.localIndex = localIndex;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;
        frame.push(frame.getLocal(localIndex), frame.getLocalDouble(localIndex));
    }

    @Override
    public int stackChange() {
        return 1;
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "slot", localIndex);
    }
}

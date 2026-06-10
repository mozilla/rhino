package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;

public class LocalClear extends Instruction {
    private final int localIndex;

    public LocalClear(int localIndex) {
        this.localIndex = localIndex;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.setLocal(localIndex, null);

        frame.pc += 1;
    }

    @Override
    public int stackChange() {
        return 0;
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "slot", localIndex);
    }
}

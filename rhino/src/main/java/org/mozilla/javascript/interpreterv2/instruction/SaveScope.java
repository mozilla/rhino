package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;

public class SaveScope extends Instruction {
    private final int exceptionScopeOffset;

    public SaveScope(int exceptionScopeOffset) {
        this.exceptionScopeOffset = exceptionScopeOffset;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        // I am unsure what this does and how it is used.
        frame.pc += 1;
        frame.saveExceptionScope(exceptionScopeOffset, frame.scope);
    }

    @Override
    public int stackChange() {
        return 0;
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "offset", exceptionScopeOffset);
    }
}

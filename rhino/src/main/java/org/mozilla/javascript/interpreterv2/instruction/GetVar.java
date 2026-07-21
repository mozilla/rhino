package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;

public class GetVar extends Instruction {
    private final int index;

    public GetVar(int index) {
        this.index = index;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;

        if (!frame.useActivation) {
            frame.push(frame.getVar(index), frame.getVarDouble(index));
        } else {
            String stringReg = frame.fnOrScript.getDescriptor().getParamOrVarName(index);
            frame.push(frame.scope.get(stringReg, frame.scope));
        }
    }

    @Override
    public int stackChange() {
        return 1;
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "index", index);
    }
}

package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.VarScope;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;

public class ResetVar extends Instruction {
    private final int index;

    public ResetVar(int index) {
        this.index = index;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        var desc = frame.fnOrScript.getDescriptor();
        if (desc.getParamOrVarConst(index) || desc.getParamOrVarLet(index)) {
            frame.setVar(index, VarScope.UNINITIALIZED);
        } else {
            frame.setVar(index, Undefined.instance);
        }
        frame.pc += 1;
    }

    @Override
    public int stackChange() {
        return 0;
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "index", index);
    }
}

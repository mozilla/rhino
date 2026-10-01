package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;

public class DefConst extends Instruction {
    private final String varName;

    public DefConst(String varName) {
        this.varName = varName;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        ScriptRuntime.defineConst(cx, frame.scope, varName);
        frame.pc += 1;
    }

    @Override
    public int stackChange() {
        return 0;
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "varName", varName);
    }
}

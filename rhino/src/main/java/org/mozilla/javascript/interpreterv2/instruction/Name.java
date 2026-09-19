package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;

public class Name extends Instruction {
    private final String name;

    public Name(String name) {
        this.name = name;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.push(ScriptRuntime.name(cx, frame.scope, name));
        frame.pc += 1;
    }

    @Override
    public int stackChange() {
        return 1;
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "name", name);
    }
}

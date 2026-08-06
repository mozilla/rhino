package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;

public class EnumId extends Instruction {
    private final int localBlockRef;

    public EnumId(int localBlockRef) {
        this.localBlockRef = localBlockRef;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        Object val = frame.getLocal(localBlockRef);
        frame.push(ScriptRuntime.enumId(val, cx));

        frame.pc += 1;
    }

    @Override
    public int stackChange() {
        return 1;
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "localBlockRef", localBlockRef);
    }
}

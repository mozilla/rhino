package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class Instanceof extends Instruction {
    private final Operand lhs;
    private final Operand rhs;

    public Instanceof(Operand lhs, Operand rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        var rhs = this.rhs.retrieve(cx, frame);
        var lhs = this.lhs.retrieve(cx, frame);
        boolean valBln = ScriptRuntime.instanceOf(lhs, rhs, cx);
        frame.push(ScriptRuntime.wrapBoolean(valBln));

        frame.pc += 1;
    }

    @Override
    public int stackChange() {
        return 1 + lhs.stackChange() + rhs.stackChange();
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "lhs", lhs, "rhs", rhs);
    }
}

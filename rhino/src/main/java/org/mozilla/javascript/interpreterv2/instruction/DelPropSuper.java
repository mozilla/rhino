package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class DelPropSuper extends Instruction {

    private final Operand lhs;
    private final Operand rhs;

    public DelPropSuper(Operand lhs, Operand rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;

        // Don't care about these values. They are passed here to potentially
        // be popped off the stack
        rhs.retrieve(cx, frame);
        lhs.retrieve(cx, frame);

        frame.push(Boolean.FALSE);
        ScriptRuntime.throwDeleteOnSuperPropertyNotAllowed();
    }

    @Override
    public int stackChange() {
        return 1 + lhs.stackChange() + rhs.stackChange();
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(
                this, "lhs", lhs.getClass().getSimpleName(), "rhs", rhs.getClass().getSimpleName());
    }
}

package org.mozilla.javascript.interpreterv2.instruction;

import static org.mozilla.javascript.InterpreterV2.doEquals;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class NotEqual extends Instruction {
    private final Operand lhs;
    private final Operand rhs;

    public NotEqual(Operand lhs, Operand rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;

        frame.push(ScriptRuntime.wrapBoolean(!doEquals(cx, frame, lhs, rhs)));
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

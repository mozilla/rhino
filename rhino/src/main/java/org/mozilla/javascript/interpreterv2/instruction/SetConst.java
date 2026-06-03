package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.VarScope;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class SetConst extends Instruction {
    private final Operand lhs;
    private final String name;
    private final Operand rhs;

    public SetConst(Operand lhs, String name, Operand rhs) {
        this.lhs = lhs;
        this.name = name;
        this.rhs = rhs;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;

        var rhs = this.rhs.retrieveAndWrap(cx, frame);
        var lhs = (VarScope) this.lhs.retrieve(cx, frame);

        frame.push(ScriptRuntime.setConst(lhs, rhs, cx, name));
    }

    @Override
    public int stackChange() {
        return 1 + lhs.stackChange() + rhs.stackChange();
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "lhs", lhs, "name", name, "rhs", rhs);
    }
}

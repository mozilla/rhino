package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class SetProp extends Instruction {
    private final Operand lhs;
    private final String property;
    private final Operand rhs;

    public SetProp(Operand lhs, String property, Operand rhs) {
        this.lhs = lhs;
        this.property = property;
        this.rhs = rhs;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;

        var rhs = this.rhs.retrieveAndWrap(cx, frame);
        var lhs = this.lhs.retrieveAndWrap(cx, frame);
        frame.push(ScriptRuntime.setObjectProp(lhs, property, rhs, cx, frame.scope));
    }

    @Override
    public int stackChange() {
        return 1 + lhs.stackChange() + rhs.stackChange();
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(
                this, "lhs", lhs, "name", property, "rhs", rhs);
    }
}

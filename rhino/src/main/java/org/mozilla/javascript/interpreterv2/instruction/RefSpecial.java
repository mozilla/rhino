package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class RefSpecial extends Instruction {
    private final Operand lhs;
    private final String property;

    public RefSpecial(Operand lhs, String property) {
        this.lhs = lhs;
        this.property = property;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;

        var obj = this.lhs.retrieveAndWrap(cx, frame);
        frame.push(ScriptRuntime.specialRef(obj, property, cx, frame.scope));
    }

    @Override
    public int stackChange() {
        return 1 + lhs.stackChange();
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "lhs", lhs, "property", property);
    }
}

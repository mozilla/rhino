package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Ref;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class SetRef extends Instruction {
    private final Operand obj;
    private final Operand rhs;

    public SetRef(Operand obj, Operand rhs) {
        this.obj = obj;
        this.rhs = rhs;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;

        var value = rhs.retrieveAndWrap(cx, frame);
        var ref = (Ref) obj.retrieve(cx, frame);
        frame.push(ScriptRuntime.refSet(ref, value, cx, frame.scope));
    }

    @Override
    public int stackChange() {
        return 1 + obj.stackChange() + rhs.stackChange();
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "obj", obj, "rhs", rhs);
    }
}

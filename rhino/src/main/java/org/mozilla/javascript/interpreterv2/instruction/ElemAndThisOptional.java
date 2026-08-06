package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class ElemAndThisOptional extends Instruction {
    private final Operand obj;
    private final Operand id;

    public ElemAndThisOptional(Operand obj, Operand id) {
        this.obj = obj;
        this.id = id;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;

        var id = this.id.retrieveAndWrap(cx, frame);
        var obj = this.obj.retrieveAndWrap(cx, frame);

        frame.push(ScriptRuntime.getElemAndThisOptional(obj, id, cx, frame.scope));
    }

    @Override
    public int stackChange() {
        return 1 + obj.stackChange() + id.stackChange();
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(
                this, "obj", obj.getClass().getSimpleName(), "id", id.getClass().getSimpleName());
    }
}

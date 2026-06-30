package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class PropAndThisOptional extends Instruction {
    private final Operand obj;
    private final String property;

    public PropAndThisOptional(Operand obj, String property) {
        this.obj = obj;
        this.property = property;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;

        var obj = this.obj.retrieveAndWrap(cx, frame);
        frame.push(ScriptRuntime.getPropAndThisOptional(obj, property, cx, frame.scope));
    }

    @Override
    public int stackChange() {
        return 1 + obj.stackChange();
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "obj", obj, "property", property);
    }
}

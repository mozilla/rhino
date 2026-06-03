package org.mozilla.javascript.interpreterv2.instruction;

import static org.mozilla.javascript.UniqueTag.DOUBLE_MARK;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class SetPropSuper extends Instruction {
    private final Operand superObject;
    private final String property;
    private final Operand rhs;

    public SetPropSuper(Operand superObject, String property, Operand rhs) {
        this.superObject = superObject;
        this.property = property;
        this.rhs = rhs;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;

        var rhs = this.rhs.retrieveAndWrap(cx, frame);
        var superObject = this.superObject.retrieve(cx, frame);
        if (superObject == DOUBLE_MARK) Kit.codeBug();

        frame.push(
                ScriptRuntime.setSuperProp(
                        superObject, property, rhs, cx, frame.scope, frame.thisObj));
    }

    @Override
    public int stackChange() {
        return 1 + superObject.stackChange() + rhs.stackChange();
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(
                this, "superObject", superObject, "property", property, "rhs", rhs);
    }
}

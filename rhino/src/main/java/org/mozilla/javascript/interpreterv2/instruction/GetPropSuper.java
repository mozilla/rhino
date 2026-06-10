package org.mozilla.javascript.interpreterv2.instruction;

import static org.mozilla.javascript.UniqueTag.DOUBLE_MARK;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class GetPropSuper extends Instruction {
    private final Operand superObject;
    private final String property;
    private final boolean noWarn;

    public GetPropSuper(Operand superObject, String property, boolean noWarn) {
        this.superObject = superObject;
        this.property = property;
        this.noWarn = noWarn;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;

        var superObject = this.superObject.retrieve(cx, frame);
        if (superObject == DOUBLE_MARK) Kit.codeBug();
        frame.push(
                ScriptRuntime.getSuperProp(
                        superObject, property, cx, frame.scope, frame.thisObj, noWarn));
    }

    @Override
    public int stackChange() {
        return 1 + superObject.stackChange();
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(
                this, "superObject", superObject, "property", property, "noWarn", noWarn);
    }
}

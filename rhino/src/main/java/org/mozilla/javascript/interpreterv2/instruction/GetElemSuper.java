package org.mozilla.javascript.interpreterv2.instruction;

import static org.mozilla.javascript.UniqueTag.DOUBLE_MARK;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class GetElemSuper extends Instruction {
    private final Operand superObject;
    private final Operand elem;

    public GetElemSuper(Operand superObject, Operand elem) {
        this.superObject = superObject;
        this.elem = elem;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;

        Object value;
        if (!elem.isDouble(frame)) {
            var elem = this.elem.retrieve(cx, frame);
            var superObject = this.superObject.retrieveAndWrap(cx, frame);
            if (superObject == DOUBLE_MARK) Kit.codeBug();
            value = ScriptRuntime.getSuperElem(superObject, elem, cx, frame.scope, frame.thisObj);
        } else {
            double d = this.elem.retrieveDouble(frame);
            var superObject = this.superObject.retrieveAndWrap(cx, frame);
            if (superObject == DOUBLE_MARK) Kit.codeBug();
            value = ScriptRuntime.getSuperIndex(superObject, d, cx, frame.scope, frame.thisObj);
        }

        frame.push(value);
    }

    @Override
    public int stackChange() {
        return 1 + superObject.stackChange() + elem.stackChange();
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(
                this, "superObject", superObject, "elem", elem);
    }
}

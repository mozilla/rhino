package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class EnumInitKeys extends Instruction {
    private final Operand obj;
    private final int index;

    public EnumInitKeys(Operand obj, int index) {
        this.obj = obj;
        this.index = index;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;

        var lhs = obj.retrieveAndWrap(cx, frame);
        var inited = ScriptRuntime.enumInit(
            lhs, cx, frame.scope,
            cx.getLanguageVersion() <= Context.VERSION_1_8
                ? ScriptRuntime.ENUMERATE_KEYS
                : ScriptRuntime.ENUMERATE_KEYS_NO_ITERATOR);
        frame.setLocal(index, inited);
    }

    @Override
    public int stackChange() {
        return obj.stackChange();
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "obj", obj, "index", index);
    }
}

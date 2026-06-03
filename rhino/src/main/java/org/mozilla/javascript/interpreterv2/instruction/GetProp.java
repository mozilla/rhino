package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class GetProp extends Instruction {
    private final Operand lhs;
    private final String property;
    private final boolean noWarn;

    public GetProp(Operand lhs, String property, boolean noWarn) {
        this.lhs = lhs;
        this.property = property;
        this.noWarn = noWarn;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;

        var lhs = this.lhs.retrieveAndWrap(cx, frame);
        frame.push(
                !noWarn
                        ? ScriptRuntime.getObjectProp(lhs, property, cx, frame.scope)
                        : ScriptRuntime.getObjectPropNoWarn(lhs, property, cx, frame.scope));
    }

    @Override
    public int stackChange() {
        return 1 + lhs.stackChange();
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(
                this, "lhs", lhs, "name", property, "nowarn", noWarn);
    }
}

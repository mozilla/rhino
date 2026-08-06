package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;

public class TemplateLiteralCallsite extends Instruction {
    private final Object[] strings;
    private Scriptable cached;

    public TemplateLiteralCallsite(Object templateLiteral) {
        this.strings = new Object[] {templateLiteral};
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        if (cached != null) {
            frame.push(cached);
        } else {
            cached = ScriptRuntime.getTemplateLiteralCallSite(cx, frame.scope, strings, 0);
            frame.push(cached);
        }
        frame.pc += 1;
    }

    @Override
    public int stackChange() {
        return 1;
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "templateLiteral", strings[0]);
    }
}

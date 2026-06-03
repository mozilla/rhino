package org.mozilla.javascript.interpreterv2.instruction;

import static org.mozilla.javascript.UniqueTag.DOUBLE_MARK;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;

public class EscXmlAttr extends Instruction {
    public static final EscXmlAttr instance = new EscXmlAttr();

    private EscXmlAttr() {}

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;
        Object value = frame.peek(0);
        if (value != DOUBLE_MARK) {
            frame.pop();
            Object result = ScriptRuntime.escapeAttributeValue(value, cx);
            frame.push(result);
        }
    }

    @Override
    public int stackChange() {
        return 0;
    }
}

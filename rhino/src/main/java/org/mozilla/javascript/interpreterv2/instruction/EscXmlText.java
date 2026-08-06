package org.mozilla.javascript.interpreterv2.instruction;

import static org.mozilla.javascript.UniqueTag.DOUBLE_MARK;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;

public class EscXmlText extends Instruction {
    public static final EscXmlText instance = new EscXmlText();

    private EscXmlText() {}

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;
        Object value = frame.peek(0);
        if (value != DOUBLE_MARK) {
            frame.pop();
            Object result = ScriptRuntime.escapeTextValue(value, cx);
            frame.push(result);
        }
    }

    @Override
    public int stackChange() {
        return 0;
    }
}

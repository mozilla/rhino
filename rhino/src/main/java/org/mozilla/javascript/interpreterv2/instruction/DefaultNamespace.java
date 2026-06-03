package org.mozilla.javascript.interpreterv2.instruction;

import static org.mozilla.javascript.UniqueTag.DOUBLE_MARK;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;

public class DefaultNamespace extends Instruction {
    public static final DefaultNamespace instance = new DefaultNamespace();

    private DefaultNamespace() {}

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;
        Object value;
        if (frame.peek(0) == DOUBLE_MARK) {
            double d = frame.popDouble();
            value = ScriptRuntime.wrapNumber(d);
        } else {
            value = frame.pop();
        }
        Object result = ScriptRuntime.setDefaultNamespace(value, cx);
        frame.push(result);
    }

    @Override
    public int stackChange() {
        return 0;
    }
}

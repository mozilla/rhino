package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.LocalScope;

public class EnterScope extends Instruction {
    public static EnterScope instance = new EnterScope();

    private EnterScope() {
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;
        frame.scope = new LocalScope(frame.scope);
        frame.push(frame.scope);
    }

    @Override
    public int stackChange() {
        return 1;
    }
}

package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.operand.Operand;

/** Arity-specialized {@link New} with no arguments. */
final class New0 extends New {
    New0(Operand fun) {
        super(fun);
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        countInvocation(cx, frame);
        doNew(cx, frame, ScriptRuntime.emptyArgs);
        frame.pc += 1;
    }

    @Override
    public int stackChange() {
        return 1 + fun.stackChange();
    }

    @Override
    public String toDebugString() {
        return formatDebug(Operand.EMPTY_ARRAY);
    }
}

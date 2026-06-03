package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.interpreterv2.operand.Operand;

/** Arity-specialized {@link New} with a single argument. */
final class New1 extends New {
    private final Operand arg0;

    New1(Operand fun, Operand arg0) {
        super(fun);
        this.arg0 = arg0;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        countInvocation(cx, frame);
        Object[] args = {arg0.retrieveAndWrap(cx, frame)};
        doNew(cx, frame, args);
        frame.pc += 1;
    }

    @Override
    public int stackChange() {
        return 1 + fun.stackChange() + arg0.stackChange();
    }

    @Override
    public String toDebugString() {
        return formatDebug(new Operand[] {arg0});
    }
}

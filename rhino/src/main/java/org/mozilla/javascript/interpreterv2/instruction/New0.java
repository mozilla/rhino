package org.mozilla.javascript.interpreterv2.instruction;

import java.util.Objects;
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
        frame.pc += 1;
        countInvocation(cx, frame);
        doNew(cx, frame, ScriptRuntime.emptyArgs);
    }

    @Override
    public int stackChange() {
        return 1 + fun.stackChange();
    }

    @Override
    public String toDebugString() {
        return formatDebug(Operand.EMPTY_ARRAY);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof New0)) {
            return false;
        }
        return Objects.equals(fun, ((New0) o).fun);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fun);
    }
}
